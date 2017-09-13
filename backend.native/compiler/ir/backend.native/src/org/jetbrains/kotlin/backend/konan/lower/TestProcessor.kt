package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.replace
import org.jetbrains.kotlin.backend.common.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.backend.common.lower.SymbolWithIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.reportCompilationWarning
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.addTopLevelInitializer
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import java.io.File

internal class TestProcessor (val context: KonanBackendContext): FileLoweringPass {

    object TEST_SUITE_CLASS: IrDeclarationOriginImpl("TEST_SUITE_CLASS")
    object TEST_SUITE_GENERATED_MEMBER: IrDeclarationOriginImpl("TEST_SUITE_GENERATED_MEMBER")

    val symbols = context.ir.symbols

    val baseClassSuiteDescriptor = symbols.baseClassSuite.descriptor

    // region Useful extensions.
    var testSuiteCnt = 0
    fun Name.synthesizeSuiteClassName() = identifier.synthesizeSuiteClassName()
    fun String.synthesizeSuiteClassName() = "${splitToSequence('.')
            .mapIndexed { i, it -> if (i != 0) it.capitalize() }
            .joinToString("")}\$test\$${testSuiteCnt++}".synthesizedName

    private val IrFile.fileName get() = name.substringAfterLast(File.separatorChar)
    private val IrFile.topLevelSuiteName get() = "Tests in file: $fileName"

    private fun <T: IrElement> IrStatementsBuilder<T>.generateFunctionRegistration(
            receiver: IrValueSymbol,
            registerTestCase: IrFunctionSymbol,
            registerFunction: IrFunctionSymbol,
            functions: Collection<TestFunction>) {
        functions.forEach {
            if (it.kind == FunctionKind.TEST) {
                // Call registerTestCase(name: String, testFunction: () -> Unit) method.
                +irCall(registerTestCase).apply {
                    dispatchReceiver = irGet(receiver)
                    putValueArgument(0, IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.builtIns.stringType,
                            it.function.descriptor.name.identifier)
                    )
                    putValueArgument(1, IrFunctionReferenceImpl(UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            descriptor.valueParameters[1].type,
                            it.function,
                            it.function.descriptor, emptyMap()))
                }
            } else {
                // Call registerFunction(kind: TestFunctionKind, () -> Unit) method.
                +irCall(registerFunction).apply {
                    dispatchReceiver = irGet(receiver)
                    val testKindEntry = it.kind.runtimeKind
                    putValueArgument(0, IrGetEnumValueImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            testKindEntry.descriptor.defaultType,
                            testKindEntry)
                    )
                    putValueArgument(1, IrFunctionReferenceImpl(UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            descriptor.valueParameters[1].type,
                            it.function,
                            it.function.descriptor, emptyMap()))
                }
            }
        }
    }
    // endregion

    // region Classes for annotation collection.
    // TODO: Support ignore
    internal enum class FunctionKind(annotationNameString: String, runtimeKindString: String) {
        TEST("kotlin.test.Test", "") {
            override val runtimeKindName: Name  get() = throw NotImplementedError()
        },

        BEFORE("kotlin.test.Before", "BEFORE"),
        AFTER("kotlin.test.After", "AFTER"),
        BEFORE_CLASS("kotlin.test.BeforeClass", "BEFORE_CLASS"),
        AFTER_CLASS("kotlin.test.AfterClass", "AFTER_CLASS");

        val annotationFqName = FqName(annotationNameString)
        open val runtimeKindName = Name.identifier(runtimeKindString)
    }

    private val FunctionKind.runtimeKind: IrEnumEntrySymbol
        get() = symbols.getTestFunctionKind(this)

    private data class TestFunction(val function: IrFunctionSymbol, val kind: FunctionKind)

    private class ClassSuite(val ownerClass: IrClassSymbol) {
        val functions = mutableListOf<TestFunction>()

        fun registerFunction(function: IrFunctionSymbol, kinds: Collection<FunctionKind>) = kinds.forEach {
            functions.add(TestFunction(function, it))
        }
    }

    private inner class AnnotationCollector : IrElementVisitorVoid {
        val testClasses = mutableMapOf<IrClassSymbol, ClassSuite>()
        val topLevelFunctions = mutableListOf<TestFunction>()

        private fun MutableMap<IrClassSymbol, ClassSuite>.getTestSuite(key: IrClassSymbol) =
                getOrPut(key) { ClassSuite(key) }

        private fun MutableMap<IrClassSymbol, ClassSuite>.getTestSuite(key: ClassDescriptor) =
                getTestSuite(symbols.symbolTable.referenceClass(key))

        private fun MutableList<TestFunction>.registerFunction(
                function: IrFunctionSymbol,
                kinds: Collection<FunctionKind>) = kinds.forEach { add(TestFunction(function, it)) }

        private fun ClassDescriptor.canContainTests() =
            !isInner && constructors.any { it.valueParameters.size == 0 } && !isAbstract()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        fun IrFunctionSymbol.hasAnnotatoin(fqName: FqName) = descriptor.annotations.any { it.fqName == fqName }

        // TODO: Use symbols instead of containingDeclaration when such information is available.
        override fun visitFunction(declaration: IrFunction) {
            val functionSymbol = declaration.symbol
            val kinds = FunctionKind.values().filter { functionSymbol.hasAnnotatoin(it.annotationFqName)  }
            if (kinds.isEmpty()) {
                return
            }
            val owner = declaration.descriptor.containingDeclaration

            when {
                owner is PackageFragmentDescriptor ->
                    topLevelFunctions.registerFunction(functionSymbol, kinds)
                owner is ClassDescriptor && owner.canContainTests() ->
                    testClasses.getTestSuite(owner).registerFunction(declaration.symbol, kinds)
                owner is ClassDescriptor && !owner.canContainTests() ->
                    context.reportCompilationWarning("Class cannot contain unit-tests: ${owner.fqNameSafe}")
                else ->
                    UnsupportedOperationException("Cannot create test function $declaration (defined in $owner")
            }
        }
    }
    // endregion

    //region Symbol and IR builders

    /** Builds the BaseClassSuite<T>.createInstance() function. */
    private inner class InstanceGetterBuilder(val testClass: IrClassSymbol, val testSuite: IrClassSymbol)
        : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrFunction>() {

        val getterName = Name.identifier("createInstance")
        val superFunction = baseClassSuiteDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(getterName, NoLookupLocation.FROM_BACKEND)
                .single { it.valueParameters.isEmpty() }

        override fun buildIr() = IrFunctionImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset =   UNDEFINED_OFFSET,
                origin =      TEST_SUITE_GENERATED_MEMBER,
                symbol =      symbol).apply {
            val builder = context.createIrBuilder(symbol)
            createParameterDeclarations()
            body = builder.irBlockBody {
                val constructor = testClass.constructors.single { it.descriptor.valueParameters.isEmpty() }
                +irReturn(irCall(constructor))
            }
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
            descriptor.initialize(
                    /* receiverParameterType        = */ null,
                    /* dispatchReceiverParameter    = */ testSuite.descriptor.thisAsReceiverParameter,
                    /* typeParameters               = */ emptyList(),
                    /* unsubstitutedValueParameters = */ emptyList(),
                    /* returnType                   = */ testClass.descriptor.defaultType,
                    /* modality                     = */ Modality.FINAL,
                    /* visibility                   = */ Visibilities.PROTECTED
            ).apply {
                overriddenDescriptors += superFunction
            }
        }

        override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ testSuite.descriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ getterName,
                        /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                        /* source                = */ SourceElement.NO_SOURCE
                )
        )
    }

    /**
     * Builds a constructor for a test suite class representing a test class (any class in the original IrFile with
     * method(s) annotated with @Test). The test suite class is a subclass of ClassTestSuite<T>
     * where T is the test class.
     */
    private inner class ClassSuiteConstructorBuilder(val testClass: IrClassSymbol,
                                                     val testSuite: IrClassSymbol,
                                                     val functions: Collection<TestFunction>)
        : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

        val suiteName = testClass.descriptor.fqNameSafe.toString()

        private fun IrClassSymbol.getFunction(name: String, predicate: (FunctionDescriptor) -> Boolean) =
                symbols.symbolTable.referenceFunction(descriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                        .single(predicate))

        override fun buildIr() = IrConstructorImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_GENERATED_MEMBER,
                symbol).apply {
            createParameterDeclarations()

            val registerTestCase = testSuite.getFunction("registerTestCase") {
                it.valueParameters.size == 2 &&
                KotlinBuiltIns.isString(it.valueParameters[0].type) &&
                it.valueParameters[1].type.isFunctionType
            }
            val registerFunction = testSuite.getFunction("registerFunction") {
                it.valueParameters.size == 2 &&
                it.valueParameters[0].type == symbols.testFunctionKind.descriptor.defaultType &&
                it.valueParameters[1].type.isFunctionType
            }

            body = context.createIrBuilder(symbol).irBlockBody {
                val superConstructor = symbols.baseClassSuiteConstructor
                +IrDelegatingConstructorCallImpl(
                        startOffset =   UNDEFINED_OFFSET,
                        endOffset =     UNDEFINED_OFFSET,
                        symbol =        symbols.symbolTable.referenceConstructor(superConstructor),
                        descriptor =    superConstructor,
                        typeArguments = mapOf(
                                superConstructor.typeParameters[0] to testClass.descriptor.defaultType
                        )
                ).apply {
                    putValueArgument(0, IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.builtIns.stringType,
                            suiteName)
                    )
                }
                // TODO: What about null here?
                generateFunctionRegistration(testSuite.owner.thisReceiver!!.symbol,
                        registerTestCase, registerFunction, functions)
            }
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
            descriptor.initialize(emptyList(), Visibilities.PUBLIC).apply {
                returnType = testSuite.descriptor.defaultType
            }
        }

        override fun buildSymbol() = IrConstructorSymbolImpl(
                ClassConstructorDescriptorImpl.createSynthesized(
                        testSuite.descriptor,
                        Annotations.EMPTY,
                        true,
                        SourceElement.NO_SOURCE
                )
        )
    }

    /**
     * Builds a test suite class representing a test class (any class in the original IrFile with method(s)
     * annotated with @Test). The test suite class is a subclass of ClassTestSuite<T> where T is the test class.
     */
    private inner class ClassSuiteBuilder(val testClass: IrClassSymbol,
                                          val containingDeclaration: DeclarationDescriptor,
                                          val functions: Collection<TestFunction>)
        : SymbolWithIrBuilder<IrClassSymbol, IrClass>() {

        val suiteClassName = testClass.descriptor.name.synthesizeSuiteClassName()
        val superType = baseClassSuiteDescriptor.defaultType.replace(listOf(testClass.descriptor.defaultType))

        val constructorBuilder = ClassSuiteConstructorBuilder(testClass,  symbol, functions)
        val instanceGetterBuilder = InstanceGetterBuilder(testClass, symbol)


        override fun buildIr() = IrClassImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_CLASS,
                symbol).apply {
            createParameterDeclarations()
            addMember(constructorBuilder.ir)
            addMember(instanceGetterBuilder.ir)
            addFakeOverrides()
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as ClassDescriptorImpl
            val constructorDescriptor = constructorBuilder.symbol.descriptor

            val contributedDescriptors = baseClassSuiteDescriptor
                    .unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map {
                        if (it == instanceGetterBuilder.superFunction) {
                            instanceGetterBuilder.symbol.descriptor
                        } else {
                            it.createFakeOverrideDescriptor(symbol.descriptor as ClassDescriptorImpl)
                        }
                    }.filterNotNull()

            descriptor.initialize(
                    SimpleMemberScope(contributedDescriptors),setOf(constructorDescriptor), constructorDescriptor
            )

            constructorBuilder.initialize()
            instanceGetterBuilder.initialize()
        }

        override fun buildSymbol(): org.jetbrains.kotlin.ir.symbols.IrClassSymbol = IrClassSymbolImpl(
                ClassDescriptorImpl(
                        /* containingDeclaration = */ containingDeclaration,
                        /* name                  = */ suiteClassName,
                        /* modality              = */ Modality.FINAL,
                        /* kind                  = */ ClassKind.CLASS,
                        /* superTypes            = */ listOf(superType),
                        /* source                = */ SourceElement.NO_SOURCE,
                        /* isExternal            = */ false
                )
        )
    }
    //endregion

    // region IR generation methods
    private fun generateClassSuite(irFile: IrFile, testClass: IrClassSymbol, functions: Collection<TestFunction>) =
            with(ClassSuiteBuilder(testClass, irFile.packageFragmentDescriptor, functions)) {
                initialize()
                irFile.declarations.add(ir)
                irFile.addTopLevelInitializer(
                        IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, ir.symbol.constructors.single())
                )
            }

    private fun generateTopLevelSuite(irFile: IrFile, functions: Collection<TestFunction>) {
        val builder = context.createIrBuilder(irFile.symbol)
        irFile.addTopLevelInitializer(builder.irBlock {
            val constructorCall = irCall(symbols.topLevelSuiteConstructor).apply {
                putValueArgument(0, IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                context.builtIns.stringType, irFile.topLevelSuiteName))
            }
            val testSuiteVal = irTemporary(constructorCall,  "topLevelTestSuite")
            generateFunctionRegistration(testSuiteVal.symbol,
                    symbols.topLevelSuiteRegisterTestCase,
                    symbols.topLevelSuiteRegisterFunction,
                    functions)
        })
    }

    // TODO: Support tests in objects
    // TODO: Support beforeTest/afterTest for companions.
    private fun createTestSuites(irFile: IrFile, annotationCollector: AnnotationCollector) {
        annotationCollector.testClasses.forEach { _, testClass ->
            generateClassSuite(irFile, testClass.ownerClass, testClass.functions)
        }
        if (annotationCollector.topLevelFunctions.isNotEmpty()) {
            generateTopLevelSuite(irFile, annotationCollector.topLevelFunctions)
        }
    }
    // endregion

    override fun lower(irFile: IrFile) {
        val annotationCollector = AnnotationCollector()
        irFile.acceptChildrenVoid(annotationCollector)
        createTestSuites(irFile, annotationCollector)
    }
}