/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDesignationEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.destructuringDeclarationContainerVariable
import org.jetbrains.kotlin.fir.declarations.isItAllowedToCallLazyResolveToTheSamePhase
import org.jetbrains.kotlin.fir.declarations.utils.componentFunctionSymbol
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.originalIfFakeOverrideOrDelegated
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * This class represents the resolver for each [FirResolvePhase].
 *
 * Usually such the resolver extends the corresponding compiler phase transformer or delegates to it.
 *
 * The main difference with original compiler transformers is that we can transform declarations
 * only under the lock of the declaration (see [LLFirLockProvider] for locks implementation).
 * E.g., to avoid [contract violations][org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirLazyResolveContractChecker]
 * we cannot transform class member declaration under the class lock – we have to take the corresponding declaration lock
 * to avoid concurrent issues.
 *
 * So, at least we have a different implementation for transformations of such declarations as [FirFile], [FirScript] and [FirRegularClass].
 *
 * Due to lazy resolution, we have to maintain the resolution order explicitly in some cases as we are not guaranteed by default that all
 * dependencies or outer declarations are resolved before the target one.
 * We have [resolveDependencies] method which describes common dependencies between declarations.
 * Also, each [LLFirResolveTarget] can define phase-specific rules.
 *
 * Implementations:
 * - [COMPILER_REQUIRED_ANNOTATIONS][FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] – [LLFirCompilerRequiredAnnotationsTargetResolver]
 * - [COMPANION_GENERATION][FirResolvePhase.COMPANION_GENERATION] – [LLFirCompanionGenerationTargetResolver]
 * - [SUPER_TYPES][FirResolvePhase.SUPER_TYPES] – [LLFirSuperTypeTargetResolver]
 * - [SEALED_CLASS_INHERITORS][FirResolvePhase.SEALED_CLASS_INHERITORS] – [LLFirSealedClassInheritorsLazyResolver]
 * - [TYPES][FirResolvePhase.TYPES] – [LLFirTypeTargetResolver]
 * - [STATUS][FirResolvePhase.STATUS] – [LLFirStatusTargetResolver]
 * - [EXPECT_ACTUAL_MATCHING][FirResolvePhase.EXPECT_ACTUAL_MATCHING] – [LLFirExpectActualMatchingTargetResolver]
 * - [CONTRACTS][FirResolvePhase.CONTRACTS] – [LLFirContractsTargetResolver]
 * - [IMPLICIT_TYPES_BODY_RESOLVE][FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] – [LLFirImplicitBodyTargetResolver]
 * - [CONSTANT_EVALUATION][FirResolvePhase.CONSTANT_EVALUATION] - [LLFirConstantEvaluationTargetResolver]
 * - [ANNOTATION_ARGUMENTS][FirResolvePhase.ANNOTATION_ARGUMENTS] – [LLFirAnnotationArgumentsTargetResolver]
 * - [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE] – [LLFirBodyTargetResolver]
 *
 * @see LLFirLockProvider
 * @see FirResolvePhase
 */
internal sealed class LLFirTargetResolver(
    protected val resolveTarget: LLFirResolveTarget,
    val resolverPhase: FirResolvePhase,
) : LLFirResolveTargetVisitor {
    val resolveTargetSession: LLFirSession get() = resolveTarget.session
    val resolveTargetScopeSession: ScopeSession get() = resolveTargetSession.getScopeSession()
    private val lockProvider: LLFirLockProvider get() = LLFirGlobalResolveComponents.getInstance(resolveTargetSession).lockProvider
    private val requiresJumpingLock: Boolean get() = resolverPhase.isItAllowedToCallLazyResolveToTheSamePhase

    private val _containingDeclarations = mutableListOf<FirDeclaration>()

    val containingDeclarations: List<FirDeclaration> get() = _containingDeclarations

    /**
     * @param context used as a context in the case of exception
     * @return the last class from [containingDeclarations]
     */
    fun containingClass(context: FirElement): FirRegularClass {
        val containingDeclaration = containingDeclarations.lastOrNull() ?: errorWithAttachment("Containing declaration is not found") {
            withFirEntry("context", context)
            withFirDesignationEntry("designation", resolveTarget.designation)
        }

        requireWithAttachment(
            containingDeclaration is FirRegularClass,
            { "${FirRegularClass::class.simpleName} expected, but ${containingDeclaration::class.simpleName} found" },
        ) {
            withFirEntry("context", context)
            withFirDesignationEntry("designation", resolveTarget.designation)
        }

        return containingDeclaration
    }

    protected inline fun withContainingDeclaration(declaration: FirDeclaration, action: () -> Unit) {
        _containingDeclarations += declaration
        try {
            action()
        } finally {
            val removed = _containingDeclarations.removeLast()
            checkWithAttachment(removed === declaration, { "Unexpected state" }) {
                withFirEntry("expected", declaration)
                withFirEntry("actual", removed)
            }
        }
    }

    /**
     * Dependency target resolution can be skipped to optimize the resolution if this phase does not require any dependencies.
     *
     * For instance, [LLFirBodyTargetResolver] skips it as no one should depend on body resolution of another declaration.
     *
     * @return **true** if [resolveDependencies] step should be skipped
     *
     * @see resolveDependencies
     */
    open val skipDependencyTargetResolutionStep: Boolean get() = false

    /**
     * Requests the resolution for dependencies to avoid race in the case of FIR instance sharing.
     * Will be executed before resolution without a lock.
     *
     * @see resolveDataClassMemberDependencies
     * @see skipDependencyTargetResolutionStep
     */
    private fun resolveDependencies(target: FirElementWithResolveState) {
        if (skipDependencyTargetResolutionStep) return

        val originalDeclaration = (target as? FirCallableDeclaration)?.originalIfFakeOverrideOrDelegated()
        when {
            // Fake or delegate declaration shared types and annotations from the original one
            originalDeclaration != null -> originalDeclaration.lazyResolveToPhase(resolverPhase)

            target is FirProperty -> {
                // We share type references and annotations with the original parameter
                target.correspondingValueParameterFromPrimaryConstructor?.lazyResolveToPhase(resolverPhase)

                // Destructuring declaration entries depends on the container property
                target.destructuringDeclarationContainerVariable?.lazyResolveToPhase(resolverPhase)
            }

            target is FirSimpleFunction && target.origin == FirDeclarationOrigin.Synthetic.DataClassMember -> {
                resolveDataClassMemberDependencies(target)
            }

            // delegate field shares the return type with the containing class
            // constructor shares types inside delegation call with the containing class
            target is FirField && target.origin == FirDeclarationOrigin.Synthetic.DelegateField || target is FirConstructor -> {
                containingClass(target).lazyResolveToPhase(resolverPhase)
            }

            // We should resolve all script parameters for consistency as they are part of the script and visible from the beginning
            target is FirScript -> target.parameters.forEach { it.lazyResolveToPhase(resolverPhase) }
        }
    }

    private fun resolveDataClassMemberDependencies(function: FirSimpleFunction) {
        when {
            /**
             * componentN method shares the return type with the corresponding property
             * also status for the componentN method transforms during the corresponding property transformation
             * [org.jetbrains.kotlin.fir.resolve.transformers.AbstractFirStatusResolveTransformer.transformProperty]
             */
            DataClassResolver.isComponentLike(function.name) -> {
                val property = containingClass(function).declarations.firstNotNullOfOrNull { declaration ->
                    (declaration as? FirProperty)?.takeIf { it.componentFunctionSymbol?.fir == function }
                }

                property?.lazyResolveToPhase(resolverPhase)
            }

            // copy method shares the return type of generated properties as the return type
            // of corresponding value parameter and annotations from them
            DataClassResolver.isCopy(function.name) -> {
                for (declaration in containingClass(function).declarations) {
                    val property = declaration as? FirProperty ?: continue
                    if (property.fromPrimaryConstructor == true) {
                        property.lazyResolveToPhase(resolverPhase)
                    } else {
                        break // all generated properties are sequential, so we can stop if we encounter another property
                    }
                }
            }
        }
    }

    final override fun withFile(firFile: FirFile, action: () -> Unit) {
        withContainingDeclaration(firFile) {
            @Suppress("DEPRECATION_ERROR")
            withContainingFile(firFile, action)
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    protected open fun withContainingFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    final override fun withScript(firScript: FirScript, action: () -> Unit) {
        withContainingDeclaration(firScript) {
            @Suppress("DEPRECATION_ERROR")
            withContainingScript(firScript, action)
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withScript", level = DeprecationLevel.ERROR)
    protected open fun withContainingScript(firScript: FirScript, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    protected open fun withContainingRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        action()
    }

    final override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        withContainingDeclaration(firClass) {
            @Suppress("DEPRECATION_ERROR")
            withContainingRegularClass(firClass, action)
        }
    }

    protected open fun checkResolveConsistency() {}

    /**
     * This method executes **not under the lock** of [target].
     * Any unsafe reads from [target] declaration have to be done under [withReadLock].
     * [performCustomResolveUnderLock] have to be used for modifications.
     *
     * This method can be useful to resolve some dependencies (like [resolveDependencies] in general),
     * but with some phase-specific rules.
     *
     * For instance, to pre-resolve [FirRegularClass] members before the class itself as it is required
     * to build the [CFG][org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph].
     *
     * @return **true** if [performCustomResolveUnderLock] has been called
     *
     * @see withReadLock
     * @see performCustomResolveUnderLock
     */
    protected open fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean = false

    /**
     * This method executes **under the lock** of [target].
     */
    protected abstract fun doLazyResolveUnderLock(target: FirElementWithResolveState)

    /**
     * Executes the resolution.
     */
    fun resolveDesignation() {
        checkResolveConsistency()
        resolveTarget.visit(this)
    }

    final override fun performAction(element: FirElementWithResolveState) {
        performResolve(element)
    }

    /**
     * Performs the resolution of [target].
     * The [target] element have to be at least in [resolverPhase].[previous][FirResolvePhase.previous] phase.
     *
     * @see resolveDependencies
     * @see doResolveWithoutLock
     * @see doLazyResolveUnderLock
     */
    protected fun performResolve(target: FirElementWithResolveState) {
        resolveDependencies(target)

        if (doResolveWithoutLock(target)) return

        if (requiresJumpingLock) {
            checkThatResolvedAtLeastToPreviousPhase(target)
            lockProvider.withJumpingLock(
                target,
                resolverPhase,
                actionUnderLock = {
                    doLazyResolveUnderLock(target)
                    updatePhaseForDeclarationInternals(target)
                },
                actionOnCycle = {
                    handleCycleInResolution(target)
                }
            )
        } else {
            performCustomResolveUnderLock(target) {
                doLazyResolveUnderLock(target)
            }
        }
    }

    /**
     * Will be executed in the case of detected cycle between elements during jumping resolve.
     *
     * **There is no guaranties that [target] is guarded by the lock of the current thread**
     *
     * @param target an element with detected cycle
     *
     * @see LLFirLockProvider.withJumpingLock
     */
    protected open fun handleCycleInResolution(target: FirElementWithResolveState) {
        errorWithFirSpecificEntries("Resolution cycle is detected", fir = target)
    }

    /**
     * Execute [action] under the write lock in the context of [target].
     *
     * Allowed only for non-jumping phases.
     *
     * @see requiresJumpingLock
     */
    protected inline fun performCustomResolveUnderLock(target: FirElementWithResolveState, crossinline action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        requireWithAttachment(!requiresJumpingLock, { "This function cannot be called with enabled jumping lock" }) {
            withFirEntry("target", target)
        }

        lockProvider.withWriteLock(target, resolverPhase) {
            action()
            updatePhaseForDeclarationInternals(target)
        }
    }

    private fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(
            target = target,
            newPhase = resolverPhase,
            updateForLocalDeclarations = resolverPhase == FirResolvePhase.BODY_RESOLVE,
        )
    }

    /**
     * Execute action under a declaration lock.
     * [action] will be executed only once in case of successful lock.
     * If some another thread is already resolved [target] declaration to [resolverPhase] then [action] won't be executed.
     */
    protected inline fun withReadLock(target: FirElementWithResolveState, action: () -> Unit) {
        checkThatResolvedAtLeastToPreviousPhase(target)
        lockProvider.withReadLock(target, resolverPhase, action)
    }

    private fun checkThatResolvedAtLeastToPreviousPhase(target: FirElementWithResolveState) {
        when (val previousPhase = resolverPhase.previous) {
            FirResolvePhase.IMPORTS -> {}
            else -> {
                target.checkPhase(previousPhase)
            }
        }
    }
}
