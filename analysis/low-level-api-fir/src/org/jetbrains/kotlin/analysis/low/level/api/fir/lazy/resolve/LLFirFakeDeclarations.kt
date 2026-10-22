/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.originalIfFakeOverrideOrDelegated

/**
 * Whether [this] declaration is a fake one.
 *
 * A fake declaration is created on the fly (usually by a scope) and does not belong to any FIR tree.
 * It shares its content with [fakeDeclarationOriginals], so it shouldn't be transformed by lazy resolvers:
 * - The shared content is resolved via [fakeDeclarationOriginals]
 * - The content which is not shared (e.g., a deferred return type of a substitution override) is computed
 *   by phase-specific rules
 * - Some content is intentionally not real (e.g., substitution overrides have STUB expressions instead of
 *   default values), so it must not be changed
 *
 * Fake declarations do not depend on outer classes during resolution, so their designation path is empty.
 *
 * @see fakeDeclarationOriginals
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirTargetResolver.doLazyResolveFakeDeclarationUnderLock
 */
internal val FirCallableDeclaration.isFakeDeclaration: Boolean
    get() = origin.isFakeDeclarationOrigin

/**
 * Declarations which share their content with [this] fake declaration.
 * They have to be resolved before [this] declaration to the same phase.
 *
 * @see isFakeDeclaration
 */
internal val FirCallableDeclaration.fakeDeclarationOriginals: List<FirElementWithResolveState>
    get() = listOfNotNull(originalIfFakeOverrideOrDelegated())

/**
 * The list is exhaustive on purpose: each new origin requires an explicit decision.
 */
private val FirDeclarationOrigin.isFakeDeclarationOrigin: Boolean
    get() = when (this) {
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.IntersectionOverride,
        is FirDeclarationOrigin.Delegated,
            // Synthetic Java declarations (like fake overrides) might have an implicit return type
        is FirDeclarationOrigin.Enhancement,
        is FirDeclarationOrigin.Synthetic.JavaProperty,
            -> true

        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.Library,
        is FirDeclarationOrigin.Precompiled,
        is FirDeclarationOrigin.BuiltIns,
        is FirDeclarationOrigin.BuiltInsFallback,
        is FirDeclarationOrigin.Java,
        is FirDeclarationOrigin.Synthetic.DataClassMember,
        is FirDeclarationOrigin.Synthetic.InlineClassMember,
        is FirDeclarationOrigin.Synthetic.FullValueClassMember,
        is FirDeclarationOrigin.Synthetic.DelegateField,
        is FirDeclarationOrigin.Synthetic.PluginFile,
        is FirDeclarationOrigin.Synthetic.Builtins,
        is FirDeclarationOrigin.Synthetic.Error,
        is FirDeclarationOrigin.Synthetic.TypeAliasConstructor,
        is FirDeclarationOrigin.Synthetic.FakeFunction,
        is FirDeclarationOrigin.Synthetic.ForwardDeclaration,
        is FirDeclarationOrigin.Synthetic.DestructuringDeclarationContainer,
        is FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk,
        is FirDeclarationOrigin.Synthetic.ImplicitWhenSubject,
        is FirDeclarationOrigin.Synthetic.ReplContainerClass,
        is FirDeclarationOrigin.Synthetic.ReplEvalFunction,
        is FirDeclarationOrigin.DynamicScope,
        is FirDeclarationOrigin.SamConstructor,
        is FirDeclarationOrigin.ImportedFromObjectOrStatic,
        is FirDeclarationOrigin.RenamedForOverride,
        is FirDeclarationOrigin.WrappedIntegerOperator,
        is FirDeclarationOrigin.ScriptCustomization,
        is FirDeclarationOrigin.FromOtherReplSnippet,
        is FirDeclarationOrigin.ForeignValue,
        is FirDeclarationOrigin.Plugin,
            -> false
    }
