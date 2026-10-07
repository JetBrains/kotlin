/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.lower.inline.AvoidLocalFOsInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createModulePhases
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.SymbolFinder
import org.jetbrains.kotlin.ir.backend.jklib.JKlibIrMangler
import org.jetbrains.kotlin.ir.classSymbol
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineDeclarationCheckerLowering
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineFunctionSerializationPreProcessing
import org.jetbrains.kotlin.ir.inline.NonReifiedTypeParameterRemappingMode
import org.jetbrains.kotlin.ir.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * The [PreSerializationSymbols] of the JKlib first phase.
 *
 * Unlike `PreSerializationKlibSymbols.Impl`, it does not eagerly look up the `kotlin.internal.SharedVariableBox` class, as
 * [jklibLoweringsOfTheFirstPhase] does not lower shared variables. The symbols that only the lowerings left out of the JKlib first phase
 * use fail loudly when they are accessed.
 */
private class JKlibPreSerializationSymbols(irBuiltIns: IrBuiltIns) : PreSerializationSymbols.Impl(irBuiltIns) {
    // Used by `KlibSyntheticAccessorGenerator` to disambiguate the synthetic accessors of private constructors.
    override val syntheticConstructorMarker: IrClassSymbol by lazy { SYNTHETIC_CONSTRUCTOR_MARKER.classSymbol() }

    // Only used by `LateinitLowering`, which the JKlib first phase does not run.
    override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.internal.throwUninitializedPropertyAccessException")

    // Not used by any lowering of the first phase.
    override val throwUnsupportedOperationException: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.internal.throwUnsupportedOperationException")

    // The coroutine symbols are only used by `InlineFunctionResolverReplacingCoroutineIntrinsics`, which the JKlib first phase does not
    // run (see `JKlibPreSerializationInlineFunctionResolver`). The consumer of the KLIB replaces the coroutine intrinsics itself.

    override val coroutineContextGetter: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.coroutines.coroutineContext")

    override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn")

    override val coroutineGetContext: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.coroutines.intrinsics.getCoroutineContext")

    private fun notNeededOnTheFirstPhase(name: String): Nothing =
        error("`$name` is not expected to be used by the lowerings of the JKlib first phase")

    private companion object {
        val SYNTHETIC_CONSTRUCTOR_MARKER = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SyntheticConstructorMarker"))
    }
}

class JKlibPreSerializationLoweringContext(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
    diagnosticReporter: IrDiagnosticReporter,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter) {
    override val symbols: PreSerializationSymbols = JKlibPreSerializationSymbols(irBuiltIns)

    // Must be the mangler that `JKlibFir2IrPipelinePhase` and `JKlibModuleSerializer` use. Otherwise, the signatures computed to look up
    // the inline function bodies in the dependency KLIBs would not match.
    override val irMangler: KotlinMangler.IrMangler = JKlibIrMangler()

    override val sharedVariablesManager: SharedVariablesManager
        get() = error("The JKlib first phase does not lower shared variables")
}

/**
 * Resolves the inline functions that the JKlib first phase inlines.
 *
 * This mirrors `PreSerializationPrivateInlineFunctionResolver` and `PreSerializationNonPrivateInlineFunctionResolver`, except that it
 * leaves alone the intrinsics that are lowered by the passes that the JKlib first phase does not run (see
 * [isIntrinsicLoweredByTheConsumer]), instead of replacing them.
 */
private open class JKlibPreSerializationInlineFunctionResolver(private val privateOnly: Boolean) : InlineFunctionResolver() {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val callee = symbol.owner.resolveFakeOverrideOrSelf()
        if (!callee.isInline) return null
        if (callee.isIntrinsicLoweredByTheConsumer()) return null
        if (privateOnly && !callee.isEffectivelyPrivate()) return null
        return callee
    }

    /**
     * Whether this function is an inline intrinsic that the consumer of the KLIB lowers itself, like the JVM backend does.
     *
     * - The `Array(size, init)` constructors, as the JKlib first phase does not run `ArrayConstructorLowering`.
     * - `arrayOf`, its primitive and unsigned variants, and `emptyArray`, which the JVM backend lowers in `VarargLowering`.
     * - The `coroutineContext` getter, whose body only throws.
     * - The `isInitialized` getter, whose body only throws, as the JKlib first phase does not run `LateinitLowering`.
     */
    private fun IrFunction.isIntrinsicLoweredByTheConsumer(): Boolean {
        if (this is IrConstructor) return hasShape(regularParameters = 2) && constructedClass.classId in ARRAY_CLASS_IDS
        if (this !is IrSimpleFunction) return false
        if (callableId in ARRAY_FACTORY_CALLABLE_IDS) return true
        // The accessor of a property is named `<get-x>`, so it has to be matched by its property.
        val propertyCallableId = correspondingPropertySymbol?.owner?.callableId ?: return false
        return propertyCallableId in INTRINSIC_PROPERTY_CALLABLE_IDS
    }

    private companion object {
        val ARRAY_CLASS_IDS: Set<ClassId> = StandardClassIds.primitiveArrayTypeByElementType.values.toSet() + StandardClassIds.Array

        val ARRAY_FACTORY_CALLABLE_IDS: Set<CallableId> =
            (PrimitiveType.entries.map { it.typeName.asString() } + UnsignedType.entries.map { it.typeName.asString() })
                .map { it.toLowerCaseAsciiOnly() + "ArrayOf" }
                .plus(listOf("arrayOf", "emptyArray"))
                .mapTo(mutableSetOf()) { CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(it)) }

        val INTRINSIC_PROPERTY_CALLABLE_IDS: Set<CallableId> = setOf(
            CallableId(StandardClassIds.BASE_COROUTINES_PACKAGE, Name.identifier("coroutineContext")),
            CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("isInitialized")),
        )
    }
}

private class JKlibPrivateInlineFunctionResolver : JKlibPreSerializationInlineFunctionResolver(privateOnly = true) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        return super.getFunctionDeclaration(symbol)?.also { function ->
            check(function.body != null) { "Unexpected inline function without body: ${function.render()}" }
        }
    }
}

/**
 * Resolves the non-private inline functions.
 *
 * The bodies of the inline functions of the dependency KLIBs are deserialized from their `inlinableFunctionsIr` component.
 */
private class JKlibNonPrivateInlineFunctionResolver(
    context: PreSerializationLoweringContext,
) : JKlibPreSerializationInlineFunctionResolver(privateOnly = false) {
    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(context.irMangler),
    )

    private val fieldSymbolBinder = UnboundFieldSymbolBinder(context.irBuiltIns.symbolFinder)

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val callee = super.getFunctionDeclaration(symbol) ?: return null
        if (callee.hasAnnotation(DO_NOT_INLINE_ON_FIRST_STAGE)) return null
        if (callee.body != null || callee !is IrSimpleFunction) return callee
        return deserializer.deserializeInlineFunction(callee)?.also(fieldSymbolBinder::bindFieldSymbolsOf)
    }

    private companion object {
        val DO_NOT_INLINE_ON_FIRST_STAGE = FqName("kotlin.internal.DoNotInlineOnFirstStage")
    }
}

/**
 * Binds the field symbols, and the class symbols qualifying them, that [NonLinkingIrInlineFunctionDeserializer] leaves unbound.
 *
 * That deserializer reads the inline function bodies of the dependencies into a symbol table of its own, and deliberately leaves the
 * symbols they reference unbound: they are not meant to be linked, as `IrFileSerializer` writes them back as signatures anyway. Field
 * symbols are the exception, because `IrFileSerializer` needs their owner to tell a field from a property's backing field.
 *
 * The other KLIB-based compilers never hit that case: a body that is serialized for cross-module inlining cannot access a Kotlin backing
 * field, only its accessors. A JKlib inline function can, however, read or write a Java field directly, e.g. `System.out`.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private class UnboundFieldSymbolBinder(private val symbolFinder: SymbolFinder) : IrVisitorVoid() {
    private val visitedFunctions = mutableSetOf<IrSimpleFunction>()

    fun bindFieldSymbolsOf(function: IrSimpleFunction) {
        // The deserializer caches the functions it deserializes, so it returns the same instance for every call site.
        if (visitedFunctions.add(function)) function.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        val symbol = expression.symbol
        if (!symbol.isBound) (symbol as IrFieldSymbolImpl).bind(symbol.findField())
        // The class qualifying an access to a static Java field, e.g. `System` in `System.out`.
        expression.superQualifierSymbol?.takeUnless { it.isBound }?.let { expression.superQualifierSymbol = it.findClass() }
        super.visitFieldAccess(expression)
    }

    /**
     * Finds the field from its signature, which is the signature of the corresponding property with the backing field marker appended.
     */
    private fun IrFieldSymbol.findField(): IrField {
        val signature = signature
        val propertySignature = (signature as? IdSignature.CompositeSignature)?.container as? IdSignature.CommonSignature
            ?: error("Cannot resolve the field of the unexpected signature `$signature`")

        val classNameSegments = propertySignature.nameSegments.dropLast(1).ifEmpty {
            error("Cannot resolve the field of the top-level property `$propertySignature`")
        }
        val classId = ClassId(FqName(propertySignature.packageFqName), FqName.fromSegments(classNameSegments), isLocal = false)
        // A Java field is represented in IR as the backing field of a property.
        return symbolFinder.findClass(classId)?.owner?.declarations
            ?.filterIsInstance<IrProperty>()
            ?.firstOrNull { it.name.asString() == propertySignature.shortName }
            ?.backingField
            ?: error("Cannot find the field `${propertySignature.declarationFqName}` of `$classId`")
    }

    private fun IrClassSymbol.findClass(): IrClassSymbol {
        val signature = signature as? IdSignature.CommonSignature
            ?: error("Cannot resolve the class of the unexpected signature `$signature`")
        val classId = ClassId(FqName(signature.packageFqName), FqName(signature.declarationFqName), isLocal = false)
        return symbolFinder.findClass(classId) ?: error("Cannot find the class `$classId`")
    }
}

private class JKlibPrivateFunctionInlining(context: PreSerializationLoweringContext) : FunctionInlining(
    context,
    JKlibPrivateInlineFunctionResolver(),
    nonReifiedTypeParameterRemappingMode = NonReifiedTypeParameterRemappingMode.SUBSTITUTE,
    substitutePureArguments = true,
)

private class JKlibNonPrivateFunctionInlining(context: PreSerializationLoweringContext) : FunctionInlining(
    context,
    JKlibNonPrivateInlineFunctionResolver(context),
    nonReifiedTypeParameterRemappingMode = NonReifiedTypeParameterRemappingMode.SUBSTITUTE,
    substitutePureArguments = true,
)

/**
 * The JKlib flavor of `loweringsOfTheFirstPhase`, with the intra-module and cross-module inliners always enabled.
 *
 * The consumer of a JKlib KLIB has Kotlin/JVM semantics. So, unlike `loweringsOfTheFirstPhase`, it does not run:
 * - `LateinitLowering` and `SharedVariablesLowering`, which the consumer runs itself, like the JVM backend does. They would also require
 *   helpers (`kotlin.internal.SharedVariableBox`, `kotlin.internal.throwUninitializedPropertyAccessException`) that a standard library
 *   with Kotlin/JVM semantics does not declare.
 * - `ArrayConstructorLowering`, as the consumer lowers the `Array(size, init)` constructors itself, like the JVM backend does.
 */
fun jklibLoweringsOfTheFirstPhase(
    languageVersionSettings: LanguageVersionSettings,
): List<NamedCompilerPhase<JKlibPreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> {
    fun createUpgradeCallableReferences(context: JKlibPreSerializationLoweringContext) =
        UpgradeCallableReferences(context, upgradeSamConversions = true)

    // The cross-module inliner already runs on the main IR tree, so the pre-processed functions do not need their own.
    fun createInlineFunctionSerializationPreProcessing(@Suppress("UNUSED_PARAMETER") context: JKlibPreSerializationLoweringContext) =
        InlineFunctionSerializationPreProcessing(
            crossModuleFunctionInliner = null,
            nonReifiedTypeParameterRemappingMode = NonReifiedTypeParameterRemappingMode.SUBSTITUTE,
        )

    fun createSyntheticAccessorGeneration(context: JKlibPreSerializationLoweringContext) =
        SyntheticAccessorLowering(context, isExecutedOnFirstPhase = true)

    val phases = buildList<(JKlibPreSerializationLoweringContext) -> ModuleLoweringPass> {
        // The inliner only handles rich callable references, so upgrade the references first, like the JS, Wasm and Native first phases do.
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs)) {
            this += ::createUpgradeCallableReferences
        }
        this += ::AvoidLocalFOsInInlineFunctionsLowering
        this += ::InlineCallCycleCheckerLowering
        this += ::LocalClassesInInlineLambdasLowering
        this += ::JKlibPrivateFunctionInlining
        this += ::InlineDeclarationCheckerLowering
        this += ::OuterThisInInlineFunctionsSpecialAccessorLowering
        this += ::createSyntheticAccessorGeneration
        this += ::JKlibNonPrivateFunctionInlining
        this += ::createInlineFunctionSerializationPreProcessing
    }
    return createModulePhases(*phases.toTypedArray())
}

/**
 * Runs the lowerings of the first compilation phase, right before the module is serialized into a KLIB.
 */
object JKlibPreSerializationLoweringPhase : PipelinePhase<JKlibFir2IrPipelineArtifact, JKlibFir2IrPipelineArtifact>(
    name = "JKlibPreSerializationLoweringPhase",
    preActions = setOf(PerformanceNotifications.IrPreLoweringStarted),
    postActions = setOf(PerformanceNotifications.IrPreLoweringFinished, CheckCompilationErrors.CheckDiagnosticCollector),
) {
    override fun executePhase(input: JKlibFir2IrPipelineArtifact): JKlibFir2IrPipelineArtifact {
        val configuration = input.configuration
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            configuration.diagnosticsCollector,
            configuration.languageVersionSettings,
        )
        val result = PhaseEngine(
            configuration.phaseConfig ?: PhaseConfig(),
            PhaserState(),
            JKlibPreSerializationLoweringContext(input.result.irBuiltIns, configuration, irDiagnosticReporter),
        ).runPreSerializationLoweringPhases(input.result, jklibLoweringsOfTheFirstPhase(configuration.languageVersionSettings))
        return input.copy(result = result)
    }
}
