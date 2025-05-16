/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer

/**
 * [KaSession], also called an *analysis session*, is the entry point to all frontend-related work. It has the following contracts:
 *
 * - It should not be accessed outside a [read action](https://plugins.jetbrains.com/docs/intellij/threading-model.html).
 * - It should only be accessed in restricted analysis mode (see `KotlinRestrictedAnalysisService` in the platform interface) when
 *   restricted analysis is allowed by the Analysis API platform.
 * - It should not be accessed from the event dispatch thread (EDT) or a write action unless explicitly allowed ([allowAnalysisOnEdt][org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt],
 *   [allowAnalysisFromWriteAction][org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction]).
 * - It should not be leaked outside the [analyze] call it was created in. To ensure that an analysis session isn't leaked, there are
 *   additional conventions, explained further below.
 * - All [lifetime owners][KaLifetimeOwner]s retrieved from an analysis session should not be leaked outside the [analyze] call that spawned
 *   the analysis session.
 *
 * To pass a lifetime owner from one `analyze` call to another, use a **pointer**:
 *
 * - [KaSymbolProvider] for [KaSymbol]s using [KaSymbol.createPointer].
 * - [KaTypePointer] for [KaType]s using [KaType.createPointer].
 *
 * To create a [KaSession], please use [analyze] or one of its siblings.
 *
 * ### Conventions to avoid leakage
 *
 * It is crucial to avoid leaking the analysis session outside the read action it was created in, as the analysis session itself and all
 * lifetime owners retrieved from it will become invalid. An analysis session also shouldn't be leaked from the [analyze] call it was
 * created in.
 *
 * It is forbidden to store an analysis session in a variable, parameter, or property. From the [analyze] block which provides the analysis
 * session, the analysis session should be passed to functions via an extension receiver, or as an ordinary parameter. For example:
 *
 * ```kotlin
 * fun KaSession.foo() { ... }
 * ```
 *
 * **Class context receivers** should not be used to pass analysis sessions. While a context receiver on a class will make the analysis
 * session available in the constructor, it will also be captured by the class as a property. This behavior is easy to miss and a high risk
 * for unintentional leakage. For example:
 *
 * ```kotlin
 * // DO NOT DO THIS
 * context(KaSession)
 * class Usage {
 *     fun foo() {
 *         // The `KaSession` is available here.
 *     }
 * }
 * ```
 *
 * ### [PsiElement] as input
 *
 * Some API components accept [PsiElement]s as input.
 * For example, [KaSymbolProvider.symbol] takes a [KtDeclaration][org.jetbrains.kotlin.psi.KtDeclaration]
 * and returns a [KaDeclarationSymbol][org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol]
 * for it.
 *
 * In this case, the symbol may be created only for elements which are a part of the current [KaSession].
 * And it means that [KaAnalysisScopeProvider.canBeAnalysed][org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider.canBeAnalysed]
 * is **true** for such elements.
 *
 * If this condition is not met, an exception will be thrown to prevent undefined behavior.
 *
 * ### Nested analysis
 *
 * While [analyze] calls can be nested, it is currently not recommended to use [lifetime owners][KaLifetimeOwner] from the outer analysis
 * context in the inner analysis context. This section illustrates the reasons behind this recommendation.
 *
 * As there is one analysis session per use-site [module][KaModule], in the best case, the analyzed element will be from the same module.
 * Then the nested [analyze] call will simply perform the analysis in the same analysis session context. As such, it *would* be possible to
 * use a symbol from the outer analysis context in the inner [analyze] call. But if it's the same use-site module, it's better to pass the
 * analysis session down the call chain directly, instead of calling [analyze] again. In addition, relying on two elements having the same
 * use-site module is an open invitation for bugs.
 *
 * In more problematic cases, nested analysis may lead to various issues. First of all, a [KaLifetimeOwner] can usually only be accessed
 * in the session where it was created. Nesting [analyze] and starting analysis from a different use-site module will effectively change the
 * current [KaSession] context. Any calls to symbols created in other sessions *will* result in an exception (unless the Analysis API
 * platform defines different accessibility rules, such as the Standalone Analysis API).
 *
 * Furthermore, even if such an access exception wasn't thrown, it is conceptually problematic to access a symbol in a different use-site
 * context. Symbols are *always* viewed from a specific use-site context. It is unclear whether the symbol would even exist in the other
 * use-site context. And even if the symbol is accessible, analyzing it may lead to different results due to differences in the use site's
 * dependencies. For example, the supertypes of a class symbol may resolve to different declaration symbols.
 *
 * In summary, using lifetime owners from an outer context in a nested [analyze] block will likely lead to an access exception given the
 * accessibility rules of lifetime owners. And even if this wasn't the case, there's a conceptual problem with using a lifetime owner in the
 * wrong session, as lifetime owners such as symbols are always viewed from a specific use-site context.
 *
 * #### Example
 *
 * ```kotlin
 * // DO NOT DO THIS
 * analyze(element1) {
 *     val symbol1 = element1.symbol
 *     analyze(element2) {
 *          val type1 = symbol1.returnType // <-- error when `element1.module` != `element2.module`
 *     }
 * }
 * ```
 */
@Suppress("DEPRECATION")
@OptIn(KaNonPublicApi::class, KaExperimentalApi::class, KaIdeApi::class)
public interface KaSession : KaLifetimeOwner,
    KaResolver,
    KaSymbolRelationProvider,
    KaDiagnosticProvider,
    KaScopeProvider,
    KaCompletionCandidateChecker,
    KaExpressionTypeProvider,
    KaTypeProvider,
    KaTypeInformationProvider,
    KaSymbolProvider,
    KaJavaInteroperabilityComponent,
    KaSymbolInformationProvider,
    KaTypeRelationChecker,
    KaExpressionInformationProvider,
    KaEvaluator,
    KaReferenceShortener,
    KaImportOptimizer,
    KaRenderer,
    KaVisibilityChecker,
    KaOriginalPsiProvider,
    KaTypeCreator,
    KaAnalysisScopeProvider,
    KaSignatureSubstitutor,
    KaResolveExtensionInfoProvider,
    KaCompilerPluginGeneratedDeclarationsProvider,
    KaCompilerFacility,
    KaSubstitutorProvider,
    KaDataFlowProvider,
    KaSourceProvider
{
    /**
     * The [KaModule] from whose perspective the analysis is performed. The use-site module defines the resolution scope of the [KaSession],
     * which signifies *where* symbols are located (such as sources, dependencies, and so on) and *which* symbols can be found in the first
     * place.
     */
    public val useSiteModule: KaModule

    /**
     * The [KaSession] of the current analysis context.
     */
    public val useSiteSession: KaSession
        get() = this

    /**
     * Returns the restored [KaSymbol] (possibly a new symbol instance) if the pointer is still valid, or `null` otherwise.
     */
    public fun <S : KaSymbol> KaSymbolPointer<S>.restoreSymbol(): S? = withValidityAssertion {
        @OptIn(KaImplementationDetail::class)
        restoreSymbol(useSiteSession)
    }

    /**
     * Returns the restored [KaType] (possibly a new type instance) if the pointer is still valid, or `null` otherwise.
     */
    public fun <T : KaType> KaTypePointer<T>.restore(): T? = withValidityAssertion {
        @OptIn(KaImplementationDetail::class)
        restore(useSiteSession)
    }
}

/**
 * Returns a [KaModule] for a given [element] in the context of the session's use-site module.
 *
 * @see KaModuleProvider.getModule
 */
public fun KaSession.getModule(element: PsiElement): KaModule =
    KaModuleProvider.getModule(useSiteModule.project, element, useSiteModule)
