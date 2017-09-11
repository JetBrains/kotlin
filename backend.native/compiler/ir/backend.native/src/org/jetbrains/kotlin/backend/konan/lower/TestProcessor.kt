package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.replace
import org.jetbrains.kotlin.backend.common.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.backend.common.lower.SymbolWithIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.reportCompilationWarning
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

internal class TestProcessor (val context: KonanBackendContext): FileLoweringPass {

    object TEST_SUITE_CLASS: IrDeclarationOriginImpl("TEST_SUITE_CLASS")
    object TEST_SUITE_GENERATED_MEMBER: IrDeclarationOriginImpl("TEST_SUITE_GENERATED_MEMBER")

    val symbols = context.ir.symbols

    val baseClassSuiteDescriptor = symbols.baseClassSuite.descriptor
    val topLevelTestSuiteDescriptor = symbols.baseTopLevelSuite.descriptor

    // TODO: Support ignore
    enum class FunctionKind(annotationNameString: String, runtimeKindString: String) {
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

    val FunctionKind.runtimeKind: IrEnumEntrySymbol
        get() = symbols.getTestFunctionKind(this)

    private data class TestFunction(val function: IrFunctionSymbol, val kind: FunctionKind)

    private class TestSuite(val owner: IrClassSymbol) {
        val functions = mutableListOf<TestFunction>()

        fun registerFunction(function: IrFunctionSymbol, kinds: Collection<FunctionKind>) = kinds.forEach {
            functions.add(TestFunction(function, it))
        }
    }

    private inner class AnnotationCollector : IrElementVisitorVoid {
        val testClasses = mutableMapOf<IrClassSymbol, TestSuite>()
        val topLevelFunctions = mutableListOf<TestFunction>()

        private fun MutableMap<IrClassSymbol, TestSuite>.getTestSuite(key: IrClassSymbol) =
                getOrPut(key) { TestSuite(key) }

        private fun MutableMap<IrClassSymbol, TestSuite>.getTestSuite(key: ClassDescriptor) =
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
                    UnsupportedOperationException("Cannot create test function for owner: $owner")
            }
        }
    }

    private fun Name.testClassName() = Name.identifier("$this\$test")

    /** Creates a [SymbolWithIrBuilder] building the BaseClassSuite<T>.createInstance() function. */
    private fun instanceGetterBuilder(testClass: IrClassSymbol, testSuite: IrClassSymbol) =
        object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrFunction>() {

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

            // TODO: add comments for parameters.
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

    /** Creates a [SymbolWithIrBuilder] building a constructor for a class test suite (BaseClassSuite subclass) */
    private fun constructorBuilder(testClass: IrClassSymbol,
                                  testSuite: IrClassSymbol,
                                  functions: Collection<TestFunction>) =
        object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            val superConstructor = baseClassSuiteDescriptor.constructors.single {
                it.valueParameters.size == 1 /* && arg[0].type == String */
            }

            override fun buildIr() = IrConstructorImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    TEST_SUITE_GENERATED_MEMBER,
                    symbol).apply {
                //createParameterDeclarations()  TODO: WHat???
                val builder = context.createIrBuilder(symbol)

                body = builder.irBlockBody {
                    // TODO: What about type args?
                    +IrDelegatingConstructorCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            symbols.symbolTable.referenceConstructor(superConstructor),
                            superConstructor,
                            mapOf(superConstructor.typeParameters[0] to testClass.descriptor.defaultType)).apply {
                        putValueArgument(0, IrConstImpl.string(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                context.builtIns.stringType,
                                testClass.descriptor.fqNameSafe.toString())
                        )
                    }
                    // TODO: Use fake overrides for calls?
                    val thisReceiver = testSuite.owner.thisReceiver!!.symbol // TODO: What about null here?
                    functions.forEach {
                        if (it.kind == FunctionKind.TEST) {
                            +irCall(symbols.registerTestCase).apply {
                                dispatchReceiver = irGet(thisReceiver)
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
                            +irCall(symbols.registerFunction).apply {
                                dispatchReceiver = irGet(thisReceiver)
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

    /** Creates a [SymbolWithIrBuilder] building a class test suite (BaseClassSuite subclass) */
    private fun classSuiteBuilder(testClass: IrClassSymbol, irFile: IrFile, functions: Collection<TestFunction>) =
        object: SymbolWithIrBuilder<IrClassSymbol, IrClass>() {

                val constructorBuilder = constructorBuilder(testClass, symbol, functions)
                val instanceGetterBuilder = instanceGetterBuilder(testClass, symbol)

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

                    val contributedDescriptors = baseClassSuiteDescriptor.unsubstitutedMemberScope
                            .getContributedDescriptors()
                            .map {
                                if (it == instanceGetterBuilder.superFunction) {
                                    instanceGetterBuilder.symbol.descriptor
                                } else {
                                    it.createFakeOverrideDescriptor(descriptor)
                                }
                            }
                            .filterNotNull()

                    descriptor.initialize(SimpleMemberScope(contributedDescriptors), setOf(constructorDescriptor), constructorDescriptor)

                    constructorBuilder.initialize()
                    instanceGetterBuilder.initialize()
                }

                override fun buildSymbol(): org.jetbrains.kotlin.ir.symbols.IrClassSymbol = IrClassSymbolImpl(
                        ClassDescriptorImpl(
                                /* containingDeclaration = */ irFile.packageFragmentDescriptor,
                                /* name                  = */ testClass.descriptor.name.testClassName(),
                                /* modality              = */ Modality.FINAL,
                                /* kind                  = */ ClassKind.CLASS,
                                /* superTypes            = */ listOf(baseClassSuiteDescriptor.defaultType.replace(listOf(testClass.descriptor.defaultType))),
                                /* source                = */ SourceElement.NO_SOURCE,
                                /* isExternal            = */ false
                        )
                )
            }

    private fun generateTestSuite(testClass: IrClassSymbol, irFile: IrFile, functions: Collection<TestFunction>) {
        // Generate class
        val builder = classSuiteBuilder(testClass, irFile, functions)
        builder.initialize()
        val irTestSuite = builder.ir
        irFile.addTopLevelInitializer(context.createIrBuilder(irFile.symbol).irBlock {
            +irTestSuite
            +IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irTestSuite.symbol.constructors.single())
        })
    }

    private fun createTestSuites(irFile: IrFile, annotationCollector: AnnotationCollector) {
        annotationCollector.testClasses.forEach { _, testClass ->
            generateTestSuite(testClass.owner, irFile, testClass.functions)
        }
        // TODO: Use another name for a file?
       // generateTestSuite(irFile.name, irFile, annotationCollector.topLevelFunctions)
    }

    override fun lower(irFile: IrFile) {
        val annotationCollector = AnnotationCollector()
        irFile.acceptChildrenVoid(annotationCollector)
        createTestSuites(irFile, annotationCollector)
    }
}