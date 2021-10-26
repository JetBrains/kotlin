/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.backend.js.utils.JsInlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isNullable

class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment,
    val additionalExportedDeclarationNames: Set<FqName>,
    override val configuration: CompilerConfiguration, // TODO: remove configuration from backend context
    override val scriptMode: Boolean = false,
    override val es6mode: Boolean = false,
    val dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    val propertyLazyInitialization: Boolean = false,
    val baseClassIntoMetadata: Boolean = false,
    val safeExternalBoolean: Boolean = false,
    val safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    override val mapping: JsMapping = JsMapping(symbolTable.irFactory),
    val granularity: JsGenerationGranularity = JsGenerationGranularity.WHOLE_PROGRAM,
) : JsCommonBackendContext {
    val fileToInitializationFuns: MutableMap<IrFile, IrSimpleFunction?> = mutableMapOf()
    val fileToInitializerPureness: MutableMap<IrFile, Boolean> = mutableMapOf()
    val fieldToInitializer: MutableMap<IrField, IrExpression> = mutableMapOf()

    val extractedLocalClasses: MutableSet<IrClass> = hashSetOf()

    override val builtIns = module.builtIns

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)

    override val irFactory: IrFactory = symbolTable.irFactory

    override var inVerbosePhase: Boolean = false

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in intrinsics.primitiveToLiteralConstructor.values ||
                call.symbol == intrinsics.arrayLiteral ||
                call.symbol == intrinsics.arrayConcat

    val devMode = configuration[JSConfigurationKeys.DEVELOPER_MODE] ?: false
    val errorPolicy = configuration[JSConfigurationKeys.ERROR_TOLERANCE_POLICY] ?: ErrorTolerancePolicy.DEFAULT

    val externalPackageFragment = mutableMapOf<IrFileSymbol, IrFile>()
    val externalDeclarations = hashSetOf<IrDeclaration>()

    val additionalExportedDeclarations = mutableSetOf<IrDeclaration>()

    val bodilessBuiltInsPackageFragment: IrPackageFragment = IrExternalPackageFragmentImpl(
        DescriptorlessExternalPackageFragmentSymbol(),
        FqName("kotlin")
    )

    val packageLevelJsModules = mutableSetOf<IrFile>()
    val declarationLevelJsModules = mutableListOf<IrDeclarationWithName>()

    private val internalPackageFragmentDescriptor = EmptyPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal"))

    private fun syntheticFile(name: String, module: IrModuleFragment): IrFile {
        return IrFileImpl(object : IrFileEntry {
            override val name = "<$name>"
            override val maxOffset = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                SourceRangeInfo(
                    "",
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET
                )

            override fun getLineNumber(offset: Int) = UNDEFINED_OFFSET
            override fun getColumnNumber(offset: Int) = UNDEFINED_OFFSET
        }, internalPackageFragmentDescriptor, module).also {
            module.files += it
        }
    }

    val testFunsPerFile = mutableMapOf<IrFile, IrSimpleFunction>()

    override fun createTestContainerFun(irFile: IrFile): IrSimpleFunction {
        return testFunsPerFile.getOrPut(irFile) {
            irFactory.addFunction(irFile) {
                name = Name.identifier("test fun")
                returnType = irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())
            }
        }
    }

    override val inlineClassesUtils = JsInlineClassesUtils(this)

    val innerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

    companion object {
        val KOTLIN_PACKAGE_FQN = FqName.fromSegments(listOf("kotlin"))


        // TODO: what is more clear way reference this getter?
        private val REFLECT_PACKAGE_FQNAME = KOTLIN_PACKAGE_FQN.child(Name.identifier("reflect"))
        private val JS_PACKAGE_FQNAME = KOTLIN_PACKAGE_FQN.child(Name.identifier("js"))
        private val JS_INTERNAL_PACKAGE_FQNAME = JS_PACKAGE_FQNAME.child(Name.identifier("internal"))

        // TODO: due to name clash those weird suffix is required, remove it once `MemberNameGenerator` is implemented
        private val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"

        val callableClosureOrigin = object : IrDeclarationOriginImpl("CALLABLE_CLOSURE_DECLARATION") {}
    }

    private val internalPackage = module.getPackage(JS_PACKAGE_FQNAME)

    override val dynamicType: IrDynamicType = IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
    override val intrinsics = JsIntrinsics(irBuiltIns, this)

    override val catchAllThrowableType: IrType
        get() = dynamicType

    override val sharedVariablesManager = JsSharedVariablesManager(this)

    override val internalPackageFqn = JS_PACKAGE_FQNAME

    private val operatorMap = referenceOperators()

    private fun primitivesWithImplicitCompanionObject(): List<Name> {
        val numbers = PrimitiveType.NUMBER_TYPES
            .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
            .map { it.typeName }

        return numbers + listOf(Name.identifier("String"), Name.identifier("Boolean"))
    }

    fun getOperatorByName(name: Name, type: IrSimpleType) = operatorMap[name]?.get(type.classifier)

    override val coroutineSymbols =
        JsCommonCoroutineSymbols(symbolTable, module, this)

    override val ir = object : Ir<JsIrBackendContext>(this, irModuleFragment) {
        override val symbols = object : Symbols<JsIrBackendContext>(this@JsIrBackendContext, irBuiltIns, symbolTable) {
            override val throwNullPointerException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_NPE"))).single())

            init {
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException"))).single())
            }

            override val throwTypeCastException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))).single())

            override val throwUninitializedPropertyAccessException =
                symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwUninitializedPropertyAccessException")).single())

            override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
                symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwKotlinNothingValueException")).single())

            override val defaultConstructorMarker =
                symbolTable.referenceClass(context.getJsInternalClass("DefaultConstructorMarker"))

            override val throwISE: IrSimpleFunctionSymbol =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))).single())

            override val stringBuilder
                get() = TODO("not implemented")
            override val coroutineImpl =
                coroutineSymbols.coroutineImpl
            override val coroutineSuspendedGetter =
                coroutineSymbols.coroutineSuspendedGetter

            private val _arraysContentEquals = getFunctions(FqName("kotlin.collections.contentEquals")).mapNotNull {
                if (it.extensionReceiverParameter != null && it.extensionReceiverParameter!!.type.isNullable())
                    symbolTable.referenceSimpleFunction(it)
                else null
            }

            // Can't use .owner until ExternalStubGenerator is invoked, hence get() = here.
            override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
                get() = _arraysContentEquals.associateBy { it.owner.extensionReceiverParameter!!.type.makeNotNull() }

            override val getContinuation = symbolTable.referenceSimpleFunction(getJsInternalFunction("getContinuation"))

            override val coroutineContextGetter = symbolTable.referenceSimpleFunction(context.coroutineSymbols.coroutineContextProperty.getter!!)

            override val suspendCoroutineUninterceptedOrReturn =
                symbolTable.referenceSimpleFunction(getJsInternalFunction(COROUTINE_SUSPEND_OR_RETURN_JS_NAME))

            override val coroutineGetContext = symbolTable.referenceSimpleFunction(getJsInternalFunction(GET_COROUTINE_CONTEXT_NAME))

            override val returnIfSuspended = symbolTable.referenceSimpleFunction(getJsInternalFunction("returnIfSuspended"))

            override val functionAdapter: IrClassSymbol
                get() = TODO("Not implemented")

            override fun functionN(n: Int): IrClassSymbol {
                return irFactory.stageController.withInitialIr { super.functionN(n) }
            }

            override fun suspendFunctionN(n: Int): IrClassSymbol {
                return irFactory.stageController.withInitialIr { super.suspendFunctionN(n) }
            }


            private val getProgressionLastElementSymbols =
                irBuiltIns.findFunctions(Name.identifier("getProgressionLastElement"), "kotlin", "internal")

            override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
                getProgressionLastElementSymbols.associateBy { it.owner.returnType.classifierOrFail }
            }

            private val toUIntSymbols = irBuiltIns.findFunctions(Name.identifier("toUInt"), "kotlin")

            override val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
                toUIntSymbols.associateBy {
                    it.owner.extensionReceiverParameter?.type?.classifierOrFail
                        ?: error("Expected extension receiver for ${it.owner.render()}")
                }
            }

            private val toULongSymbols = irBuiltIns.findFunctions(Name.identifier("toULong"), "kotlin")

            override val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
                toULongSymbols.associateBy {
                    it.owner.extensionReceiverParameter?.type?.classifierOrFail
                        ?: error("Expected extension receiver for ${it.owner.render()}")
                }
            }
        }

        override fun unfoldInlineClassType(irType: IrType): IrType? {
            return inlineClassesUtils.getInlinedClass(irType)?.typeWith()
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    // classes forced to be loaded

    val errorCodeSymbol: IrSimpleFunctionSymbol? =
        if (errorPolicy.allowErrors) symbolTable.referenceSimpleFunction(getJsInternalFunction("errorCode")) else null

    override val primitiveClassesObject = getIrClass(FqName("kotlin.reflect.js.internal.PrimitiveClasses"))

    val throwableClass = getIrClass(JsIrBackendContext.KOTLIN_PACKAGE_FQN.child(Name.identifier("Throwable")))

    val primitiveCompanionObjects = primitivesWithImplicitCompanionObject().associateWith {
        getIrClass(JS_INTERNAL_PACKAGE_FQNAME.child(Name.identifier("${it.identifier}CompanionObject")))
    }



    // Top-level functions forced to be loaded


    val coroutineEmptyContinuation = symbolTable.referenceProperty(
        getProperty(
            FqName.fromSegments(
                listOf(
                    "kotlin",
                    "coroutines",
                    "js",
                    "internal",
                    "EmptyContinuation"
                )
            )
        )
    )


    val newThrowableSymbol = symbolTable.referenceSimpleFunction(getJsInternalFunction("newThrowable"))
    val extendThrowableSymbol = symbolTable.referenceSimpleFunction(getJsInternalFunction("extendThrowable"))
    val setPropertiesToThrowableInstanceSymbol =
        symbolTable.referenceSimpleFunction(getJsInternalFunction("setPropertiesToThrowableInstance"))

    val throwISEsymbol = symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))).single())
    val throwIAEsymbol = symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_IAE"))).single())

    override val suiteFun = getFunctions(FqName("kotlin.test.suite")).singleOrNull()?.let { symbolTable.referenceSimpleFunction(it) }
    override val testFun = getFunctions(FqName("kotlin.test.test")).singleOrNull()?.let { symbolTable.referenceSimpleFunction(it) }

    val throwableConstructors by lazy2 { throwableClass.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol } }
    val defaultThrowableCtor by lazy2 { throwableConstructors.single { !it.owner.isPrimary && it.owner.valueParameters.size == 0 } }

    val kpropertyBuilder = getFunctions(FqName("kotlin.js.getPropertyCallableRef")).single().let { symbolTable.referenceSimpleFunction(it) }
    val klocalDelegateBuilder =
        getFunctions(FqName("kotlin.js.getLocalDelegateReference")).single().let { symbolTable.referenceSimpleFunction(it) }

    private fun referenceOperators(): Map<Name, MutableMap<IrClassifierSymbol, IrSimpleFunctionSymbol>> {
        val primitiveIrSymbols = irBuiltIns.primitiveIrTypes.map { it.classifierOrFail as IrClassSymbol }

        return OperatorNames.ALL.map { name ->
            // TODO to replace KotlinType with IrType we need right equals on IrType
            name to primitiveIrSymbols.fold(mutableMapOf<IrClassifierSymbol, IrSimpleFunctionSymbol>()) { m, s ->
                val function = s.owner.declarations.filterIsInstance<IrSimpleFunction>().singleOrNull { it.name == name }
                function?.let { m.put(s, it.symbol) }
                m
            }
        }.toMap()
    }

    private fun findProperty(memberScope: MemberScope, name: Name): List<PropertyDescriptor> =
        memberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND).toList()

    internal fun getJsInternalClass(name: String): ClassDescriptor =
        findClass(internalPackage.memberScope, Name.identifier(name))

    internal fun getClass(fqName: FqName): ClassDescriptor =
        findClass(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    internal fun getProperty(fqName: FqName): PropertyDescriptor =
        findProperty(module.getPackage(fqName.parent()).memberScope, fqName.shortName()).single()

    private fun getIrClass(fqName: FqName): IrClassSymbol = symbolTable.referenceClass(getClass(fqName))

    internal fun getJsInternalFunction(name: String): SimpleFunctionDescriptor =
        findFunctions(internalPackage.memberScope, Name.identifier(name)).singleOrNull() ?: error("Internal function '$name' not found")

    fun getFunctions(fqName: FqName): List<SimpleFunctionDescriptor> =
        findFunctions(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    override fun log(message: () -> String) {
        /*TODO*/
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}
