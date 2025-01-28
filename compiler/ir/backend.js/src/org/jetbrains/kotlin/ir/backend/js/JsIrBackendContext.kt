/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsPolyfills
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.translateJsCodeIntoStatementList
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.filterIsInstanceMapNotNull
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val additionalExportedDeclarationNames: Set<FqName>,
    keep: Set<String>,
    override val configuration: CompilerConfiguration, // TODO: remove configuration from backend context
    val mainCallArguments: List<String>?,
    val dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    val safeExternalBoolean: Boolean = false,
    val safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    override val mapping: JsMapping = JsMapping(),
    val granularity: JsGenerationGranularity = JsGenerationGranularity.WHOLE_PROGRAM,
    val incrementalCacheEnabled: Boolean = false,
) : JsCommonBackendContext {
    val phaseConfig = configuration.phaseConfig ?: PhaseConfig()

    override val allowExternalInlining: Boolean
        get() = true

    val polyfills = JsPolyfills()
    val globalIrInterner = IrInterningService()

    val localClassNames = WeakHashMap<IrClass, String>()
    val classToItsId = WeakHashMap<IrClass, String>()

    val minimizedNameGenerator: MinimizedNameGenerator =
        MinimizedNameGenerator()

    val keeper: Keeper =
        Keeper(keep)

    val fieldDataCache = WeakHashMap<IrClass, Map<IrField, String>>()

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)

    override val irFactory: IrFactory = symbolTable.irFactory

    override var inVerbosePhase: Boolean = false

    val devMode = configuration[JSConfigurationKeys.DEVELOPER_MODE] ?: false
    override val es6mode = configuration[JSConfigurationKeys.USE_ES6_CLASSES] ?: false
    val platformArgumentsProviderJsExpression = configuration[JSConfigurationKeys.DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS]

    override val externalPackageFragment = mutableMapOf<IrFileSymbol, IrFile>()

    override val additionalExportedDeclarations = hashSetOf<IrDeclaration>()

    override val bodilessBuiltInsPackageFragment: IrPackageFragment = IrExternalPackageFragmentImpl(
        DescriptorlessExternalPackageFragmentSymbol(),
        FqName("kotlin")
    )

    val packageLevelJsModules = hashSetOf<IrFile>()
    val declarationLevelJsModules = mutableListOf<IrDeclarationWithName>()

    val testFunsPerFile = hashMapOf<IrFile, IrSimpleFunction>()

    override val inlineClassesUtils = JsInlineClassesUtils(this)

    override val innerClassesSupport: InnerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

    private val internalPackage = module.getPackage(JsStandardClassIds.BASE_JS_PACKAGE)

    val dynamicType: IrDynamicType = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)
    val intrinsics: JsIntrinsics = JsIntrinsics(irBuiltIns)

    override val reflectionSymbols: ReflectionSymbols get() = intrinsics.reflectionSymbols

    override val propertyLazyInitialization: PropertyLazyInitialization = PropertyLazyInitialization(
        enabled = configuration.get(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true),
        eagerInitialization = symbolTable.descriptorExtension.referenceClass(getJsInternalClass("EagerInitialization"))
    )

    override val catchAllThrowableType: IrType
        get() = dynamicType

    override val internalPackageFqn = JsStandardClassIds.BASE_JS_PACKAGE

    override val sharedVariablesManager = JsSharedVariablesManager(this.irBuiltIns, this.dynamicType, this.intrinsics)

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
                candidates.singleOrNull { it.owner.valueParameters[0].type.classifierOrNull == rhsType.classifier }
        }

    override val jsPromiseSymbol: IrClassSymbol?
        get() = intrinsics.promiseClassSymbol

    override val enumEntries = getIrClass(StandardClassIds.BASE_ENUMS_PACKAGE.child(Name.identifier("EnumEntries")))
    override val createEnumEntries = getFunctions(StandardClassIds.BASE_ENUMS_PACKAGE.child(Name.identifier("enumEntries")))
        .find { it.valueParameters.firstOrNull()?.type?.isFunctionType == false }
        .let { symbolTable.descriptorExtension.referenceSimpleFunction(it!!) }

    override val symbols = JsSymbols(irBuiltIns, irFactory.stageController, intrinsics)
    override val ir = object : Ir() {
        override val symbols = this@JsIrBackendContext.symbols
        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    // classes forced to be loaded

    val throwableClass = getIrClass(StandardClassIds.Throwable.asSingleFqName())

    val primitiveCompanionObjects = primitivesWithImplicitCompanionObject().associateWith {
        getIrClass(JsStandardClassIds.BASE_JS_INTERNAL_PACKAGE.child(Name.identifier("${it.identifier}CompanionObject")))
    }

    // Top-level functions forced to be loaded


    val coroutineEmptyContinuation = symbolTable.descriptorExtension.referenceProperty(
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


    val newThrowableSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("newThrowable"))
    val extendThrowableSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("extendThrowable"))
    val setPropertiesToThrowableInstanceSymbol =
        symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("setPropertiesToThrowableInstance"))

    override val suiteFun = getFunctions(FqName("kotlin.test.suite")).singleOrNull()?.let {
        symbolTable.descriptorExtension.referenceSimpleFunction(it)
    }
    override val testFun = getFunctions(FqName("kotlin.test.test")).singleOrNull()?.let {
        symbolTable.descriptorExtension.referenceSimpleFunction(it)
    }

    val throwableConstructors by lazy(LazyThreadSafetyMode.NONE) {
        throwableClass.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol }
    }
    val defaultThrowableCtor by lazy(LazyThreadSafetyMode.NONE) { throwableConstructors.single { !it.owner.isPrimary && it.owner.valueParameters.size == 0 } }

    val kpropertyBuilder = getFunctions(FqName("kotlin.js.getPropertyCallableRef")).single().let {
        symbolTable.descriptorExtension.referenceSimpleFunction(it)
    }
    val klocalDelegateBuilder =
        getFunctions(FqName("kotlin.js.getLocalDelegateReference")).single().let {
            symbolTable.descriptorExtension.referenceSimpleFunction(it)
        }

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

    internal fun getIrClass(fqName: FqName): IrClassSymbol = symbolTable.descriptorExtension.referenceClass(getClass(fqName))

    internal fun getJsInternalFunction(name: String): SimpleFunctionDescriptor =
        findFunctions(internalPackage.memberScope, Name.identifier(name)).singleOrNull() ?: error("Internal function '$name' not found")

    fun getFunctions(fqName: FqName): List<SimpleFunctionDescriptor> =
        findFunctions(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    private fun parseJsFromAnnotation(declaration: IrDeclaration, annotationClassId: ClassId): Pair<IrConstructorCall, JsFunction>? {
        val annotation = declaration.getAnnotation(annotationClassId.asSingleFqName())
            ?: return null
        val jsCode = annotation.getValueArgument(0)
            ?: compilationException("@${annotationClassId.shortClassName} annotation must contain the JS code argument", annotation)
        val statements = translateJsCodeIntoStatementList(jsCode, declaration)
            ?: compilationException("Could not parse JS code", annotation)
        val parsedJsFunction = statements.singleOrNull()
            ?.safeAs<JsExpressionStatement>()
            ?.expression
            ?.safeAs<JsFunction>()
            ?: compilationException("Provided JS code is not a js function", annotation)
        return annotation to parsedJsFunction
    }

    private val outlinedJsCodeFunctions = WeakHashMap<IrFunctionSymbol, JsFunction>()
    fun getJsCodeForFunction(symbol: IrFunctionSymbol): JsFunction? {
        val originalSymbol = symbol.owner.originalFunction.symbol
        val jsFunction = outlinedJsCodeFunctions[originalSymbol]
        if (jsFunction != null) return jsFunction

        parseJsFromAnnotation(originalSymbol.owner, JsStandardClassIds.Annotations.JsOutlinedFunction)
            ?.let { (annotation, parsedJsFunction) ->
                val sourceMap = (annotation.getValueArgument(1) as? IrConst)?.value as? String
                val parsedSourceMap = sourceMap?.let { parseSourceMap(it, originalSymbol.owner.fileOrNull, annotation) }
                if (parsedSourceMap != null) {
                    val remapper = SourceMapLocationRemapper(parsedSourceMap)
                    remapper.remap(parsedJsFunction)
                }
                outlinedJsCodeFunctions[originalSymbol] = parsedJsFunction
                return parsedJsFunction
            }

        parseJsFromAnnotation(originalSymbol.owner, JsStandardClassIds.Annotations.JsFun)
            ?.let { (_, parsedJsFunction) ->
                outlinedJsCodeFunctions[originalSymbol] = parsedJsFunction
                return parsedJsFunction
            }
        return null
    }

    private fun parseSourceMap(sourceMap: String, file: IrFile?, annotation: IrConstructorCall): SourceMap? {
        if (sourceMap.isEmpty()) return null
        return when (val result = SourceMapParser.parse(sourceMap)) {
            is SourceMapSuccess -> result.value
            is SourceMapError -> {
                reportWarning(
                    """
                    Invalid source map in annotation:
                    ${annotation.dumpKotlinLike()}
                    ${result.message.replaceFirstChar(Char::uppercase)}.
                    Some debug information may be unavailable.
                    If you believe this is not your fault, please create an issue: https://kotl.in/issue
                    """.trimIndent(),
                    file,
                    annotation,
                )
                null
            }
        }
    }

    override val partialLinkageSupport = createPartialLinkageSupportForLowerings(
        configuration.partialLinkageConfig,
        irBuiltIns,
        configuration.messageCollector
    )

    internal var nextAssociatedObjectKey = 0
}
