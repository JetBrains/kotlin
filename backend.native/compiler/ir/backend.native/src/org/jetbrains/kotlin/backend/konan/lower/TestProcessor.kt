package org.jetbrains.kotlin.backend.konan.lower

import kotlinx.coroutines.experimental.newCoroutineContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.replace
import org.jetbrains.kotlin.backend.common.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.backend.common.lower.SymbolWithIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.reportWarning
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
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
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
import org.jetbrains.kotlin.serialization.KonanIr
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

    private fun MutableList<TestFunction>.registerFunction(
            function: IrFunctionSymbol,
            kinds: Collection<FunctionKind>) = kinds.forEach { add(TestFunction(function, it)) }

    private fun MutableList<TestFunction>.registerFunction(function: IrFunctionSymbol, kind: FunctionKind) =
            add(TestFunction(function, kind))

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
    // TODO: Rework containing checks.
    internal enum class FunctionKind(annotationNameString: String, runtimeKindString: String) {
        TEST("kotlin.test.Test", "") {
            override val runtimeKindName: Name  get() = throw NotImplementedError()
            override fun canBeIn(classDescriptor: ClassDescriptor, context: KonanBackendContext) =
                    classDescriptor.checkInstanceFunctionAnnotation(annotationFqName, context)
        },

        BEFORE("kotlin.test.Before", "BEFORE") {
            override fun canBeIn(classDescriptor: ClassDescriptor, context: KonanBackendContext) =
                    classDescriptor.checkInstanceFunctionAnnotation(annotationFqName, context)
        },
        AFTER("kotlin.test.After", "AFTER") {
            override fun canBeIn(classDescriptor: ClassDescriptor, context: KonanBackendContext) =
                    classDescriptor.checkInstanceFunctionAnnotation(annotationFqName, context)
        },
        BEFORE_CLASS("kotlin.test.BeforeClass", "BEFORE_CLASS") {
            override fun canBeIn(classDescriptor: ClassDescriptor, context: KonanBackendContext) =
                    classDescriptor.checkCompanionFunctionAnnotation(annotationFqName, context)
        },
        AFTER_CLASS("kotlin.test.AfterClass", "AFTER_CLASS") {
            override fun canBeIn(classDescriptor: ClassDescriptor, context: KonanBackendContext) =
                    classDescriptor.checkCompanionFunctionAnnotation(annotationFqName, context)
        };

        val annotationFqName = FqName(annotationNameString)
        open val runtimeKindName = Name.identifier(runtimeKindString)
        abstract fun canBeIn(classDescriptor: ClassDescriptor, context: KonanBackendContext): Boolean

        companion object {
            private fun KonanBackendContext.warn(msg: String) = reportCompilationWarning(msg)

            private fun ClassDescriptor.checkInstanceFunctionAnnotation(annotation: FqName,
                                                                        context: KonanBackendContext) =
                    when {
                        isInner -> {
                            context.warn("Annotation $annotation is not allowed for methods of an inner class")
                            false
                        }
                        isAbstract() -> {
                            context.warn("Annotation $annotation is not allowed for methods of an abstract class")
                            false
                        }
                        isCompanionObject -> {
                            context.warn("Annotation $annotation is not allowed for methods of a companion object")
                            false
                        }
                        constructors.none { it.valueParameters.size == 0 } -> {
                            context.warn("Test class has no default constructor: ${fqNameSafe}")
                            false
                        }
                        else -> true
                    }

            private fun ClassDescriptor.checkCompanionFunctionAnnotation(annotation: FqName,
                                                                         context: KonanBackendContext): Boolean {
                if (!isCompanionObject) {
                    context.warn("Annotation $annotation is only allowed for methods of a companion object")
                    return false
                }
                return true
            }
        }
    }

    private val FunctionKind.runtimeKind: IrEnumEntrySymbol
        get() = symbols.getTestFunctionKind(this)

    private data class TestFunction(val function: IrFunctionSymbol, val kind: FunctionKind)

    private inner class TestClass(val ownerClass: IrClassSymbol) {
        var companion: IrClassSymbol? = null
        val functions = mutableListOf<TestFunction>()

        val hasCompanionFunctions get() = companion != null

        fun registerFunction(function: IrFunctionSymbol, kinds: Collection<FunctionKind>) =
                functions.registerFunction(function, kinds)
        fun registerFUnction(function: IrFunctionSymbol, kind: FunctionKind) =
                functions.registerFunction(function, kind)
    }

    private inner class AnnotationCollector : IrElementVisitorVoid {
        val testClasses = mutableMapOf<IrClassSymbol, TestClass>()
        val topLevelFunctions = mutableListOf<TestFunction>()

        private fun MutableMap<IrClassSymbol, TestClass>.getTestClass(key: IrClassSymbol) =
                getOrPut(key) { TestClass(key) }

        private fun MutableMap<IrClassSymbol, TestClass>.getTestClass(key: ClassDescriptor) =
                getTestClass(symbols.symbolTable.referenceClass(key))

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        fun IrFunctionSymbol.hasAnnotatoin(fqName: FqName) = descriptor.annotations.any { it.fqName == fqName }

        fun registerClassFunction(classDescriptor: ClassDescriptor,
                                  function: IrFunctionSymbol,
                                  kinds: Collection<FunctionKind>) {
            kinds.filter { it.canBeIn(classDescriptor, context) }.forEach {
                when (it) {
                    FunctionKind.TEST, FunctionKind.BEFORE, FunctionKind.AFTER ->
                            testClasses.getTestClass(classDescriptor).registerFUnction(function, it)
                    FunctionKind.AFTER_CLASS, FunctionKind.BEFORE_CLASS -> {
                        assert(classDescriptor.isCompanionObject)
                        val containingClass = classDescriptor.containingDeclaration as ClassDescriptor
                        val testClass = testClasses.getTestClass(containingClass)
                        testClass.companion = symbols.symbolTable.referenceClass(classDescriptor)
                        testClass.registerFUnction(function, it)
                    }
                }
            }
        }

        // TODO: Use symbols instead of containingDeclaration when such information is available.
        override fun visitFunction(declaration: IrFunction) {
            val symbol = declaration.symbol
            val owner = declaration.descriptor.containingDeclaration

            val kinds = FunctionKind.values().filter { symbol.hasAnnotatoin(it.annotationFqName)  }
            if (kinds.isEmpty()) {
                return
            }

            when (owner) {
                is PackageFragmentDescriptor -> topLevelFunctions.registerFunction(symbol, kinds)
                is ClassDescriptor -> registerClassFunction(owner, symbol, kinds)
                else -> UnsupportedOperationException("Cannot create test function $declaration (defined in $owner")
            }
        }
    }
    // endregion

    //region Symbol and IR builders

    private inner class CompanionGetterBuilder(val testCompanion: IrClassSymbol, val testSuite: IrClassSymbol)
        : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrFunction>() {

        val getterName = Name.identifier("getCompanion")
        val superFunction = baseClassSuiteDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(getterName, NoLookupLocation.FROM_BACKEND)
                .single { it.valueParameters.isEmpty() }

        override fun buildIr(): IrFunction = IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_GENERATED_MEMBER,
                symbol).apply {
            val builder = context.createIrBuilder(symbol)
            createParameterDeclarations()
            body = builder.irBlockBody {
                +irReturn(IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        testCompanion.descriptor.defaultType, testCompanion)
                )
            }
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
            descriptor.initialize(
                    null,
                    testSuite.descriptor.thisAsReceiverParameter,
                    emptyList(),
                    emptyList(),
                    testCompanion.descriptor.defaultType,
                    Modality.FINAL,
                    Visibilities.PROTECTED
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
    private inner class ClassSuiteConstructorBuilder(val suiteName: String,
                                                     val testClassType: KotlinType,
                                                     val testCompanionType: KotlinType,
                                                     val testSuite: IrClassSymbol,
                                                     val functions: Collection<TestFunction>)
        : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

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
                        typeArguments = mapOf(superConstructor.typeParameters[0] to testClassType,
                                superConstructor.typeParameters[1] to testCompanionType)
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
                                          val testCompanion: IrClassSymbol?,
                                          val containingDeclaration: DeclarationDescriptor,
                                          val functions: Collection<TestFunction>)
        : SymbolWithIrBuilder<IrClassSymbol, IrClass>() {

        val suiteName = testClass.descriptor.fqNameSafe.toString()
        val suiteClassName = testClass.descriptor.name.synthesizeSuiteClassName()

        val testClassType = testClass.descriptor.defaultType
        val testCompanionType = testCompanion?.descriptor?.defaultType ?: context.irBuiltIns.nothing

        val superType = baseClassSuiteDescriptor.defaultType.replace(listOf(testClassType, testCompanionType))

        val constructorBuilder = ClassSuiteConstructorBuilder(
                suiteName, testClassType,testCompanionType, symbol, functions
        )
        val instanceGetterBuilder = InstanceGetterBuilder(testClass, symbol)
        val companionGetterBuilder = testCompanion?.let { CompanionGetterBuilder(it, symbol) }


        override fun buildIr() = IrClassImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                TEST_SUITE_CLASS,
                symbol).apply {
            createParameterDeclarations()
            addMember(constructorBuilder.ir)
            addMember(instanceGetterBuilder.ir)
            companionGetterBuilder?.let { addMember(it.ir) }
            addFakeOverrides()
        }

        override fun doInitialize() {
            val descriptor = symbol.descriptor as ClassDescriptorImpl
            val constructorDescriptor = constructorBuilder.symbol.descriptor

            val contributedDescriptors = baseClassSuiteDescriptor
                    .unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map {
                        when {
                            it == instanceGetterBuilder.superFunction -> instanceGetterBuilder.symbol.descriptor
                            it == companionGetterBuilder?.superFunction -> companionGetterBuilder.symbol.descriptor
                            else -> it.createFakeOverrideDescriptor(symbol.descriptor as ClassDescriptorImpl)
                        }
                    }.filterNotNull()

            descriptor.initialize(
                    SimpleMemberScope(contributedDescriptors),setOf(constructorDescriptor), constructorDescriptor
            )

            constructorBuilder.initialize()
            instanceGetterBuilder.initialize()
            companionGetterBuilder?.initialize()
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
    private fun generateClassSuite(irFile: IrFile, testClass: TestClass) =
            with(ClassSuiteBuilder(testClass.ownerClass,
                    testClass.companion,
                    irFile.packageFragmentDescriptor,
                    testClass.functions)) {
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

    // TODO: Get tests from Kotlin/JVM
    // TODO: Support tests in objects
    // TODO: Check beforeTest/afterTest for companions.
    private fun createTestSuites(irFile: IrFile, annotationCollector: AnnotationCollector) {
        annotationCollector.testClasses.filter {
            it.value.functions.any { it.kind == FunctionKind.TEST }
        }.forEach { _, testClass ->
            generateClassSuite(irFile, testClass)
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