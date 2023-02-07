/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsPolyfills
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.translateJsCodeIntoStatementList
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.web.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.WeakHashMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val additionalExportedDeclarationNames: Set<FqName>,
    keep: Set<String>,
    override val configuration: CompilerConfiguration, // TODO: remove configuration from backend context
    override val es6mode: Boolean = false,
    val dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    val safeExternalBoolean: Boolean = false,
    val safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    override val mapping: JsMapping = JsMapping(),
    val granularity: JsGenerationGranularity = JsGenerationGranularity.WHOLE_PROGRAM,
    val incrementalCacheEnabled: Boolean = false
) : JsCommonBackendContext {
    override val scriptMode: Boolean get() = false

    val polyfills = JsPolyfills()
    val fieldToInitializer = WeakHashMap<IrField, IrExpression>()

    val localClassNames = WeakHashMap<IrClass, String>()

    val minimizedNameGenerator: MinimizedNameGenerator =
        MinimizedNameGenerator()

    val keeper: Keeper =
        Keeper(keep)

    val fieldDataCache = WeakHashMap<IrClass, Map<IrField, String>>()

    override val builtIns = module.builtIns

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)

    override val irFactory: IrFactory = symbolTable.irFactory

    override var inVerbosePhase: Boolean = false

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in intrinsics.primitiveToLiteralConstructor.values ||
                call.symbol == intrinsics.arrayLiteral ||
                call.symbol == intrinsics.arrayConcat

    val devMode = configuration[JSConfigurationKeys.DEVELOPER_MODE] ?: false
    val errorPolicy = configuration[WebConfigurationKeys.ERROR_TOLERANCE_POLICY] ?: ErrorTolerancePolicy.DEFAULT

    val externalPackageFragment = mutableMapOf<IrFileSymbol, IrFile>()

    val additionalExportedDeclarations = mutableSetOf<IrDeclaration>()

    val bodilessBuiltInsPackageFragment: IrPackageFragment = IrExternalPackageFragmentImpl(
        DescriptorlessExternalPackageFragmentSymbol(),
        FqName("kotlin")
    )

    val packageLevelJsModules = mutableSetOf<IrFile>()
    val declarationLevelJsModules = mutableListOf<IrDeclarationWithName>()

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
        private val ENUMS_PACKAGE_FQNAME = KOTLIN_PACKAGE_FQN.child(Name.identifier("enums"))
        private val JS_POLYFILLS_PACKAGE = JS_PACKAGE_FQNAME.child(Name.identifier("polyfill"))
        private val JS_INTERNAL_PACKAGE_FQNAME = JS_PACKAGE_FQNAME.child(Name.identifier("internal"))

        // TODO: due to name clash those weird suffix is required, remove it once `MemberNameGenerator` is implemented
        private val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
    }

    private val internalPackage = module.getPackage(JS_PACKAGE_FQNAME)

    val dynamicType: IrDynamicType = IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
    val intrinsics: JsIntrinsics = JsIntrinsics(irBuiltIns, this)

    override val reflectionSymbols: ReflectionSymbols get() = intrinsics.reflectionSymbols

    override val propertyLazyInitialization: PropertyLazyInitialization = PropertyLazyInitialization(
        enabled = configuration.get(WebConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true),
        eagerInitialization = symbolTable.referenceClass(getJsInternalClass("EagerInitialization"))
    )

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

    fun getOperatorByName(name: Name, lhsType: IrSimpleType, rhsType: IrSimpleType?) =
        operatorMap[name]?.get(lhsType.classifier)?.let { candidates ->
            if (rhsType == null)
                candidates.singleOrNull()
            else
                candidates.singleOrNull { it.owner.valueParameters[0].type.cast<IrSimpleType>().classifier == rhsType.classifier }
        }

    override val coroutineSymbols =
        JsCommonCoroutineSymbols(symbolTable, module, this)

    override val enumEntries = getIrClass(ENUMS_PACKAGE_FQNAME.child(Name.identifier("EnumEntries")))
    override val createEnumEntries = getFunctions(ENUMS_PACKAGE_FQNAME.child(Name.identifier("enumEntries")))
        .find { it.valueParameters.firstOrNull()?.type?.isFunctionType == true }
        .let { symbolTable.referenceSimpleFunction(it!!) }

    override val ir = object : Ir<JsIrBackendContext>(this) {
        override val symbols = object : Symbols(irBuiltIns, symbolTable) {
            private val context = this@JsIrBackendContext

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

            override val continuationClass = context.coroutineSymbols.continuationClass

            override val coroutineContextGetter =
                symbolTable.referenceSimpleFunction(context.coroutineSymbols.coroutineContextProperty.getter!!)

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

    private fun referenceOperators(): Map<Name, Map<IrClassSymbol, Collection<IrSimpleFunctionSymbol>>> {
        val primitiveIrSymbols = irBuiltIns.primitiveIrTypes.map { it.classifierOrFail as IrClassSymbol }
        return OperatorNames.ALL.associateWith { name ->
            primitiveIrSymbols.associateWith { classSymbol ->
                classSymbol.owner.declarations
                    .filterIsInstanceMapNotNull<IrSimpleFunction, IrSimpleFunctionSymbol> { function ->
                        function.symbol.takeIf { function.name == name }
                    }
            }
        }
    }

    private fun findProperty(memberScope: MemberScope, name: Name): List<PropertyDescriptor> =
        memberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND).toList()

    internal fun getJsInternalClass(name: String): ClassDescriptor =
        findClass(internalPackage.memberScope, Name.identifier(name))

    internal fun getClass(fqName: FqName): ClassDescriptor =
        findClass(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    internal fun getProperty(fqName: FqName): PropertyDescriptor =
        findProperty(module.getPackage(fqName.parent()).memberScope, fqName.shortName()).single()

    internal fun getIrClass(fqName: FqName): IrClassSymbol = symbolTable.referenceClass(getClass(fqName))

    internal fun getJsInternalFunction(name: String): SimpleFunctionDescriptor =
        findFunctions(internalPackage.memberScope, Name.identifier(name)).singleOrNull() ?: error("Internal function '$name' not found")

    internal fun getJsInternalProperty(name: String): PropertyDescriptor =
        findProperty(internalPackage.memberScope, Name.identifier(name)).singleOrNull() ?: error("Internal function '$name' not found")


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

    private val outlinedJsCodeFunctions = WeakHashMap<IrFunctionSymbol, JsFunction>()

    fun addOutlinedJsCode(symbol: IrSimpleFunctionSymbol, outlinedJsCode: JsFunction) {
        outlinedJsCodeFunctions[symbol] = outlinedJsCode
    }

    fun getJsCodeForFunction(symbol: IrFunctionSymbol): JsFunction? {
        val jsFunction = outlinedJsCodeFunctions[symbol]
        if (jsFunction != null) return jsFunction
        val jsFunAnnotation = symbol.owner.getAnnotation(JsAnnotations.jsFunFqn) ?: return null
        val jsCode = jsFunAnnotation.getValueArgument(0)
            ?: compilationException("@JsFun annotation must contain an argument", jsFunAnnotation)
        val statements = translateJsCodeIntoStatementList(jsCode, this, symbol.owner)
            ?: compilationException("Could not parse JS code", jsFunAnnotation)
        val parsedJsFunction = statements.singleOrNull()
            ?.safeAs<JsExpressionStatement>()
            ?.expression
            ?.safeAs<JsFunction>()
            ?: compilationException("Provided JS code is not a js function", jsFunAnnotation)
        outlinedJsCodeFunctions[symbol] = parsedJsFunction
        return parsedJsFunction
    }
}
