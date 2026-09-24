/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file adds the "first compilation phase" (a.k.a. pre-serialization) lowering phase to the JKlib pipeline, which is
// what makes `-Xklib-ir-inliner` meaningful for JKlib.
//
// The stock `loweringsOfTheFirstPhase()` cannot be reused as-is because it unconditionally runs `SharedVariablesLowering`
// and `LateinitLowering`, which:
//   * require `kotlin.internal.SharedVariableBox*` / `kotlin.internal.throwUninitialized*`, none of which exist in the
//     JKlib standard library, and
//   * would box captured mutable locals, which is a plain regression for JKlib, whose consumer models captured mutable
//     locals natively.
// So this file defines a JKlib-tailored variant of that list, still gated on the exact same language features so that
// `-Xklib-ir-inliner` keeps driving it.
@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
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
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.jklib.JKlibIrMangler
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineDeclarationCheckerLowering
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineFunctionSerializationPreProcessing
import org.jetbrains.kotlin.ir.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.ir.validation.IrValidationError
import org.jetbrains.kotlin.ir.validation.IrValidationSeverity
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

/**
 * The [PreSerializationSymbols] of the JKlib target.
 *
 * Only the symbols that are actually reachable from [jklibLoweringsOfTheFirstPhase] are resolvable.
 * The others deliberately fail loudly, because the JKlib standard library does not declare them, and
 * silently resolving them to something else would produce wrong code.
 */
private class JKlibPreSerializationSymbols(irBuiltIns: IrBuiltIns) : PreSerializationSymbols.Impl(irBuiltIns) {

    /** Used by `KlibSyntheticAccessorGenerator` to disambiguate generated private constructors. */
    override val syntheticConstructorMarker: IrClassSymbol by lazy {
        symbolFinder.findClass(SYNTHETIC_CONSTRUCTOR_MARKER)
            ?: error("Class $SYNTHETIC_CONSTRUCTOR_MARKER is not found in the standard library")
    }

    override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.internal.throwUninitializedPropertyAccessException")

    /** Used by `UpgradeCallableReferences` to build wrappers for local delegated properties. */
    override val throwUnsupportedOperationException: IrSimpleFunctionSymbol by lazy {
        symbolFinder.findFunctions(THROW_UNSUPPORTED_OPERATION_EXCEPTION).singleOrNull()
            ?: error("Function $THROW_UNSUPPORTED_OPERATION_EXCEPTION is not found in the standard library")
    }

    override val coroutineContextGetter: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.coroutines.coroutineContext")

    override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn")

    override val coroutineGetContext: IrSimpleFunctionSymbol
        get() = notNeededOnTheFirstPhase("kotlin.coroutines.intrinsics.getCoroutineContext")

    private fun notNeededOnTheFirstPhase(name: String): Nothing =
        error("`$name` is not expected to be used by the JKlib lowerings of the first phase")

    private companion object {
        val SYNTHETIC_CONSTRUCTOR_MARKER =
            ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SyntheticConstructorMarker"))

        val THROW_UNSUPPORTED_OPERATION_EXCEPTION =
            CallableId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("throwUnsupportedOperationException"))
    }
}

/** The [PreSerializationLoweringContext] of the JKlib target. */
class JKlibPreSerializationLoweringContext(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
    diagnosticReporter: IrDiagnosticReporter,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter) {

    override val symbols: PreSerializationSymbols = JKlibPreSerializationSymbols(irBuiltIns)

    // Must be the very same mangler that `JKlibFir2IrPipelinePhase` and `JKlibModuleSerializer` use, otherwise the
    // signatures computed to look up inline function bodies in dependency KLIBs would not match.
    override val irMangler: KotlinMangler.IrMangler = JKlibIrMangler()

    override val sharedVariablesManager: SharedVariablesManager
        get() = error("JKlib does not lower shared variables: captured mutable locals are modeled natively by its consumer")
}

/**
 * Resolves the inline functions that the first compilation phase is allowed to inline for JKlib.
 *
 * This mirrors `PreSerialization{Private,NonPrivate}InlineFunctionResolver`, which are `internal` to `ir.inline`,
 * plus the exclusion of the few intrinsics that the consumer of the JKlib IR lowers itself.
 */
private open class JKlibPreSerializationInlineFunctionResolver(private val privateOnly: Boolean) : InlineFunctionResolver() {

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val callee = symbol.owner.resolveFakeOverrideOrSelf()
        if (!callee.isInline) return null
        if (callee.isHandledByConsumer()) return null
        if (privateOnly && !callee.isEffectivelyPrivate()) return null
        return callee
    }

    /**
     * The consumer of the JKlib IR lowers a few `kotlin` intrinsics itself rather than through inlining, so they must be
     * left intact.
     */
    private fun IrFunction.isHandledByConsumer(): Boolean {
        if (this !is IrSimpleFunction) return false
        // `arrayOf`/`xArrayOf`/`emptyArray` are lowered by the consumer.
        if (callableId in ARRAY_FACTORY_CALLABLE_IDS) return true
        // The accessor of a property is named `<get-x>`, so it has to be matched on its property.
        val propertyCallableId = correspondingPropertySymbol?.owner?.callableId ?: return false
        return propertyCallableId in INTRINSIC_PROPERTY_CALLABLE_IDS
    }

    private companion object {
        val ARRAY_FACTORY_CALLABLE_IDS: Set<CallableId> =
            (PrimitiveType.entries.map { it.typeName.asString() } + UnsignedType.entries.map { it.typeName.asString() })
                .map { it.toLowerCaseAsciiOnly() + "ArrayOf" }
                .plus(listOf("arrayOf", "emptyArray"))
                .mapTo(mutableSetOf()) { CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(it)) }

        /**
         * The properties whose getter is an intrinsic: their body only throws, and the lowering that replaces the calls
         * to them runs on the second phase.
         */
        val INTRINSIC_PROPERTY_CALLABLE_IDS: Set<CallableId> =
            setOf(
                CallableId(StandardClassIds.BASE_COROUTINES_PACKAGE, Name.identifier("coroutineContext")),
                CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("isInitialized")),
            )
    }
}

/** Resolves only private inline functions, which are always inlined on the first phase. */
private class JKlibPrivateInlineFunctionResolver : JKlibPreSerializationInlineFunctionResolver(privateOnly = true) {

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? =
        super.getFunctionDeclaration(symbol)?.also {
            check(it.body != null) { "Unexpected inline function without body: ${it.render()}" }
        }
}

/**
 * Resolves non-private inline functions.
 *
 * When [inlineCrossModuleFunctions] is set, bodiless inline functions coming from dependency KLIBs are deserialized on
 * demand from their `inlinableFunctionsIr` component. Dependencies that were not compiled with the IR inliner simply do
 * not have that component, in which case their calls are left un-inlined for the second phase to deal with.
 */
private class JKlibNonPrivateInlineFunctionResolver(
    context: PreSerializationLoweringContext,
    private val inlineCrossModuleFunctions: Boolean,
) : JKlibPreSerializationInlineFunctionResolver(privateOnly = false) {

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(context.irMangler),
        symbolResolver = JKlibInlineFunctionSymbolResolver(context)::resolve,
    )

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val callee = super.getFunctionDeclaration(symbol) ?: return null
        if (callee.hasAnnotation(DO_NOT_INLINE_ON_FIRST_STAGE)) return null
        if (callee.body != null || callee !is IrSimpleFunction) return callee
        if (!inlineCrossModuleFunctions) return null
        // Unlike the other KLIB backends, JKlib can depend on Kotlin libraries that are only available as plain JVM jars.
        // Those have no `inlinableFunctionsIr` to deserialize from, and the deserializer would throw on them, so leave
        // their calls for the second phase to inline.
        if (callee.containerSource !is KlibDeserializedContainerSource) return null
        return deserializer.deserializeInlineFunction(callee)
    }

    private companion object {
        val DO_NOT_INLINE_ON_FIRST_STAGE = FqName("kotlin.internal.DoNotInlineOnFirstStage")
    }
}

private class JKlibPrivateFunctionInlining(context: PreSerializationLoweringContext) :
    FunctionInlining(context, JKlibPrivateInlineFunctionResolver())

private class JKlibNonPrivateFunctionInlining(
    context: PreSerializationLoweringContext,
    inlineCrossModuleFunctions: Boolean,
) : FunctionInlining(context, JKlibNonPrivateInlineFunctionResolver(context, inlineCrossModuleFunctions))

/**
 * Reports the symbols that the first phase left unbound.
 *
 * The other KLIB backends cannot run this check: they deliberately leave the references of the inlined bodies unbound
 * and let the consumer of the KLIB link them. The JKlib pipeline resolves them eagerly instead (see
 * [JKlibInlineFunctionSymbolResolver]), so it can require that nothing is left behind.
 *
 * Unlike the other IR validations, this one is not gated behind `-Xverify-ir`. It checks the single invariant that the
 * eager resolution above is responsible for, and a violation of it means the IR refers to a declaration that was never
 * linked. Only `isBound` is checked, so this costs one traversal rather than the whole of IR validation.
 */
private class JKlibValidateNoUnboundSymbols(private val context: JKlibPreSerializationLoweringContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        validateIr(
            irModule,
            context.irBuiltIns,
            IrValidatorConfig(checkUnboundSymbols = true),
            context.diagnosticReporter,
            getSeverity = { error -> runIf(error.cause == IrValidationError.Cause.UnboundSymbol) { IrValidationSeverity.ERROR } },
            phaseName = "JKlibValidateNoUnboundSymbols",
        )
    }
}

/**
 * The JKlib flavor of `loweringsOfTheFirstPhase`.
 *
 * Both language features are set by `-Xklib-ir-inliner` (see
 * `K2JKlibCompilerArgumentsConfigurator.configureExtraLanguageFeatures`).
 */
fun jklibLoweringsOfTheFirstPhase(
    languageVersionSettings: LanguageVersionSettings,
): List<NamedCompilerPhase<JKlibPreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> {
    val inlineIntraModule = languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)
    val inlineCrossModuleFunctions =
        languageVersionSettings.supportsFeature(LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization)

    fun createInlineNonPrivateFunctionsPhase(context: JKlibPreSerializationLoweringContext) =
        JKlibNonPrivateFunctionInlining(context, inlineCrossModuleFunctions)

    fun createInlineFunctionSerializationPreProcessing(
        context: JKlibPreSerializationLoweringContext,
    ): InlineFunctionSerializationPreProcessing {
        // If cross-module inlining already happened in the main IR tree there is nothing left to do for the copies that
        // are about to be serialized. Otherwise, run it on the copies only, so that consumers of this KLIB get fully
        // inlined bodies.
        val inliner = runUnless(inlineCrossModuleFunctions) { JKlibNonPrivateFunctionInlining(context, true) }
        return InlineFunctionSerializationPreProcessing(crossModuleFunctionInliner = inliner)
    }

    fun createSyntheticAccessorGeneration(context: JKlibPreSerializationLoweringContext) =
        SyntheticAccessorLowering(context, isExecutedOnFirstPhase = true)

    // A factory is needed because the default parameters make `::UpgradeCallableReferences` resolve to the wrong
    // function type.
    fun createUpgradeCallableReferences(context: JKlibPreSerializationLoweringContext) =
        UpgradeCallableReferences(context, upgradeSamConversions = true)

    val phases = buildList<(JKlibPreSerializationLoweringContext) -> ModuleLoweringPass> {
        // The inliner only understands `IrRichFunctionReference`. FIR2IR still produces the legacy
        // `IrFunctionExpression`/`IrFunctionReference` nodes, so they have to be upgraded first, exactly like the JS,
        // Wasm and Native first phases do. Without this, inline lambdas are not inlined at their call site inside the
        // inlined function body.
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs)) {
            this += ::createUpgradeCallableReferences
        }
        this += ::AvoidLocalFOsInInlineFunctionsLowering
        this += ::InlineCallCycleCheckerLowering
        if (inlineIntraModule) {
            this += ::LocalClassesInInlineLambdasLowering
            // Hoist the local classes declared in inline function bodies out of those bodies, so that every call site
            // shares one class instead of getting its own copy. The other KLIB backends do not do this, but JKlib
            // preserves the Kotlin/JVM class identity semantics.
            this += ::JKlibLocalClassesInInlineFunctionsLowering
            this += ::JKlibLocalClassesExtractionFromInlineFunctionsLowering
            // `Array(size) { ... }` is an inline constructor. It must be rewritten before the inliner gets a chance to
            // inline it.
            this += ::ArrayConstructorLowering
            this += ::JKlibPrivateFunctionInlining
            this += ::InlineDeclarationCheckerLowering
            this += ::OuterThisInInlineFunctionsSpecialAccessorLowering
            this += ::createSyntheticAccessorGeneration
            this += ::createInlineNonPrivateFunctionsPhase
            this += ::createInlineFunctionSerializationPreProcessing
        } else {
            this += ::InlineDeclarationCheckerLowering
        }
        this += ::JKlibValidateNoUnboundSymbols
    }
    return createModulePhases(*phases.toTypedArray())
}

/**
 * Runs the lowerings of the first compilation phase, right after FIR2IR.
 */
object JKlibPreSerializationLoweringPhase : PipelinePhase<JKlibFir2IrPipelineArtifact, JKlibFir2IrPipelineArtifact>(
    name = "JKlibPreSerializationLoweringPhase",
    preActions = setOf(PerformanceNotifications.IrPreLoweringStarted),
    postActions = setOf(
        PerformanceNotifications.IrPreLoweringFinished,
        CheckCompilationErrors.CheckDiagnosticCollector,
    ),
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
        ).runPreSerializationLoweringPhases(
            input.result,
            jklibLoweringsOfTheFirstPhase(configuration.languageVersionSettings),
        )

        return input.copy(result = result)
    }
}
