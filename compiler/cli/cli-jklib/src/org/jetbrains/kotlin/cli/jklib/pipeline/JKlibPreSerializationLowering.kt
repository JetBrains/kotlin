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
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.classSymbol
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineDeclarationCheckerLowering
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineFunctionSerializationPreProcessing
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideOrSelf
import org.jetbrains.kotlin.ir.validation.IrValidationError
import org.jetbrains.kotlin.ir.validation.IrValidationSeverity
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.runIf

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
    // TODO(KT-90406): Return the right symbol once `LateinitLowering` runs on the first phase.
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
    val resolvedLibraries: List<KotlinLibrary>,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter) {
    override val symbols: PreSerializationSymbols = JKlibPreSerializationSymbols(irBuiltIns)

    // Must be the mangler that `JKlibFir2IrPipelinePhase` and `JKlibModuleSerializer` use. Otherwise, the signatures computed to look up
    // the inline function bodies in the dependency KLIBs would not match.
    override val irMangler: KotlinMangler.IrMangler = JvmIrMangler

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
private open class JKlibPreSerializationInlineFunctionResolver(private val inlineMode: InlineMode) : InlineFunctionResolver() {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val callee = symbol.owner.resolveFakeOverrideOrSelf()
        if (!callee.isInline) return null
        if (callee.isIntrinsicLoweredByTheConsumer()) return null
        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !callee.isEffectivelyPrivate()) return null
        return callee
    }

    // TODO(KT-90406): Stop leaving the `Array(size, init)` constructors and the `isInitialized` getter to the consumer once the JKlib
    //  first phase runs `ArrayConstructorLowering` and `LateinitLowering`.
    /**
     * Whether this function is an inline intrinsic that the consumer of the KLIB lowers itself, like the JVM backend does.
     *
     * - The `Array(size, init)` constructors, as the JKlib first phase does not run `ArrayConstructorLowering`.
     * - `arrayOf`, its primitive and unsigned variants, and `emptyArray`, which the JVM backend lowers in `JvmBuiltInsLowering`.
     * - The `coroutineContext` getter, whose body only throws.
     * - The `isInitialized` getter, whose body only throws, as the JKlib first phase does not run `LateinitLowering`.
     */
    private fun IrFunction.isIntrinsicLoweredByTheConsumer(): Boolean {
        if (this is IrConstructor) return hasShape(regularParameters = 2) && constructedClass.classId in ARRAY_CLASS_IDS
        // All these intrinsics are top-level declarations, and `callableId` fails for the members of local classes.
        if (this !is IrSimpleFunction || parent !is IrPackageFragment) return false
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

private class JKlibPrivateInlineFunctionResolver : JKlibPreSerializationInlineFunctionResolver(InlineMode.PRIVATE_INLINE_FUNCTIONS) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        return super.getFunctionDeclaration(symbol)?.also { function ->
            check(function.body != null) { "Unexpected inline function without body: ${function.render()}" }
        }
    }
}

/**
 * Resolves the non-private inline functions.
 *
 * The JKlib first phase always inlines the inline functions of the other modules: their bodies are deserialized from the
 * `inlinableFunctionsIr` component of the dependency KLIBs.
 */
private class JKlibNonPrivateInlineFunctionResolver(
    context: JKlibPreSerializationLoweringContext,
) : JKlibPreSerializationInlineFunctionResolver(InlineMode.ALL_INLINE_FUNCTIONS) {
    private val symbolResolver = JKlibInlineFunctionSymbolResolver(context)

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(context.irMangler),
        symbolResolver = symbolResolver::resolve,
    )

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val callee = super.getFunctionDeclaration(symbol) ?: return null
        if (callee.hasAnnotation(DO_NOT_INLINE_ON_FIRST_STAGE)) return null
        if (callee.body != null || callee !is IrSimpleFunction) return callee
        return deserializer.deserializeInlineFunction(callee)
    }

    private companion object {
        val DO_NOT_INLINE_ON_FIRST_STAGE = FqName("kotlin.internal.DoNotInlineOnFirstStage")
    }
}

private class JKlibPrivateFunctionInlining(context: PreSerializationLoweringContext) :
    FunctionInlining(context, JKlibPrivateInlineFunctionResolver())

private class JKlibNonPrivateFunctionInlining(context: JKlibPreSerializationLoweringContext) :
    FunctionInlining(context, JKlibNonPrivateInlineFunctionResolver(context))

/**
 * Reports the symbols that the first phase left unbound.
 *
 * The other KLIB-based backends can't run this check: they deliberately leave the references of the inlined bodies unbound and let
 * the consumer of the KLIB link them. The JKlib pipeline resolves them eagerly instead (see `JKlibInlineFunctionSymbolResolver`),
 * so it can require that nothing is left unbound.
 *
 * Unlike the other IR validations, this one isn't gated behind `-Xverify-ir`. It checks the single invariant that the eager
 * resolution above is responsible for, and a violation of it means that the IR refers to a declaration that was never linked.
 * Only [org.jetbrains.kotlin.ir.symbols.IrSymbol.isBound] is checked, so this costs one traversal rather than the whole IR validation.
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
        InlineFunctionSerializationPreProcessing(crossModuleFunctionInliner = null)

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
        this += ::JKlibValidateNoUnboundSymbols
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
            JKlibPreSerializationLoweringContext(input.result.irBuiltIns, configuration, irDiagnosticReporter, input.resolvedLibraries),
        ).runPreSerializationLoweringPhases(input.result, jklibLoweringsOfTheFirstPhase(configuration.languageVersionSettings))
        return input.copy(result = result)
    }
}
