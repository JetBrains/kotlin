/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
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
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsPolyfills
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.translateJsCodeIntoStatementList
import org.jetbrains.kotlin.ir.backend.js.utils.JsInlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.utils.Keeper
import org.jetbrains.kotlin.ir.backend.js.utils.MinimizedNameGenerator
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.filterIsInstanceMapNotNull
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    override val symbolTable: SymbolTable,
    val additionalExportedDeclarationNames: Set<FqName>,
    keep: Set<String>,
    override val configuration: CompilerConfiguration, // TODO: remove configuration from backend context
    val mainCallArguments: List<String>?,
    val dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    val safeExternalBoolean: Boolean = false,
    val safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
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

    override val innerClassesSupport: InnerClassesSupport = JsInnerClassesSupport(irFactory)

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
                candidates.singleOrNull { candidate ->
                    candidate.owner.parameters.first { it.kind == IrParameterKind.Regular }.type.classifierOrNull == rhsType.classifier
                }
        }

    override val jsPromiseSymbol: IrClassSymbol?
        get() = intrinsics.promiseClassSymbol

    override val enumEntries = getIrClass(StandardClassIds.BASE_ENUMS_PACKAGE.child(Name.identifier("EnumEntries")))
    override val createEnumEntries = getFunctions(StandardClassIds.BASE_ENUMS_PACKAGE.child(Name.identifier("enumEntries")))
        .find { it.valueParameters.firstOrNull()?.type?.isFunctionType == false }
        .let { symbolTable.descriptorExtension.referenceSimpleFunction(it!!) }

    override val symbols = JsSymbols(irBuiltIns, irFactory.stageController, intrinsics)

    override val sharedVariablesManager = KlibSharedVariablesManager(symbols)

    override val shouldGenerateHandlerParameterForDefaultBodyFun: Boolean
        get() = true

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


    override val suiteFun = getFunctions(FqName("kotlin.test.suite")).singleOrNull()?.let {
        symbolTable.descriptorExtension.referenceSimpleFunction(it)
    }
    override val testFun = getFunctions(FqName("kotlin.test.test")).singleOrNull()?.let {
        symbolTable.descriptorExtension.referenceSimpleFunction(it)
    }

    val newThrowableSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("newThrowable"))
    val extendThrowableSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("extendThrowable"))
    val setupCauseParameterSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("setupCauseParameter"))
    val setPropertiesToThrowableInstanceSymbol = symbolTable.descriptorExtension.referenceSimpleFunction(getJsInternalFunction("setPropertiesToThrowableInstance"))

    val throwableConstructors by lazy(LazyThreadSafetyMode.NONE) {
        throwableClass.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol }
    }
    val defaultThrowableCtor by lazy(LazyThreadSafetyMode.NONE) {
        throwableConstructors.single { !it.owner.isPrimary && it.owner.parameters.isEmpty() }
    }
    val throwableConstructorWithMessageOnly by lazy(LazyThreadSafetyMode.NONE) {
        throwableConstructors.single { it.owner.parameters.size == 1 && it.owner.parameters[0].type.isNullableString() }
    }
    val throwableConstructorWithBothMessageAndCause by lazy(LazyThreadSafetyMode.NONE) {
        throwableConstructors.single { it.owner.parameters.size == 2 }
    }

    val kpropertyBuilder = getFunctions(FqName("kotlin.js.getPropertyCallableRef2")).single().let {
        symbolTable.descriptorExtension.referenceSimpleFunction(it)
    }
    val klocalDelegateBuilder =
        getFunctions(FqName("kotlin.js.getLocalDelegateReference2")).single().let {
            symbolTable.descriptorExtension.referenceSimpleFunction(it)
        }
    val throwLinkageErrorInCallableNameSymbol = getFunctions(FqName("kotlin.js.throwLinkageErrorInCallableName")).single().let {
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
        val jsCode = annotation.arguments[0]
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
                val sourceMap = (annotation.arguments[1] as? IrConst)?.value as? String
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

    /**
     * We try to eliminate interface metadata as much as possible in the generated JS code.
     *
     * Generally, we are able to completely eliminate any traces of an interface from the final artifact,
     * provided that the interface does not participate in reflection (i.e. there are no `MyInterface::class` expressions), has
     * no associated objects, and is never cast to (including `is`-checks). Let's call such an interface _ethereal_, and all the other
     * interfaces _real_.
     *
     * At runtime, _real_ interfaces are always represented by an integer we call _interface ID_. However, depending on certain factors,
     * that interface ID may be complimented with more runtime information.
     *
     * We use the following logic for deciding which parts of interface metadata to emit:
     * #### Without incremental compilation
     * - If an interface is _real_, we assign that interface a unique identifier **at compile time**.
     *   For classes that implement this interface, we generate an `initMetadataForClassWithStaticInterfaceMask` call,
     *   to which we pass an array of integers (also generated at compile time) that represents a bit mask of all the _real_ interfaces that
     *   it implements. In the bit mask, the N-th bit is set iff the class implements the interface whose _interface ID_ equals to N.
     * - `is`-checks for such interfaces are lowered into `kotlin.js.isInterfaceImpl` calls, where we pass the corresponding interface ID as
     *   an integer literal.
     * - If the interface participates in reflection or has associated objects,
     *   then for such interface we additionally generate an `initMetadataForInterfaceId` call, to which we pass the interface ID as
     *   an integer literal, the interface name (if the interface participates in reflection) and a map of its associated objects
     *   (if there are any). The interface metadata is stored in a global map.
     *   `MyInterface::class` expressions are lowered into `kotlin.js.getKClassForInterfaceId` calls.
     * #### With incremental compilation
     * If an interface is _real_, then for classes implementing this interface we generate
     * an `initMetadataForClassWithDynamicInterfaceMask` call, to which we pass an array of _interface constructors_ instead of a bit mask.
     * An interface constructor is just a JS function with empty body which also has a `$metadata$` property, where the interface runtime
     * metadata is stored.
     *
     * For the interface itself we generate an `initMetadataForInterface` call. This function will generate an interface ID for this
     * interface at runtime, and store the interface ID and other metadata in the interface constructor's `$metadata$` property.
     *
     * **Q:** Why do we use different logic for when incremental compilation is enabled and for when it's not?
     * **A:** Because we can only ensure the uniqueness of interface IDs generated at compile time if the current compiler invocation operates on
     * the whole world, which is not the case for incremental compilation.
     */
    internal val supportsInterfaceMetadataStripping: Boolean
        get() = !incrementalCacheEnabled

    private var nextInterfaceId = 0

    internal fun getInterfaceId(iface: IrClass): Int {
        if (!supportsInterfaceMetadataStripping) {
            compilationException("Interface metadata stripping is not supported", iface)
        }
        if (!iface.isInterface) {
            compilationException("Type is not an interface", iface)
        }
        iface.interfaceId?.let { return it }
        return nextInterfaceId++.also {
            iface.interfaceId = it
        }
    }

    internal var nextAssociatedObjectKey = 0
}
