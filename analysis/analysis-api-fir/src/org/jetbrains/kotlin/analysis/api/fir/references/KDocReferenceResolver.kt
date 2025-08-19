/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.components.deprecationStatus
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.getLongestExistingPackageScope
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.getNestedScopePossiblyContainingShortName
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.getTypeQualifiedExtensions
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.possibleGetMethodNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.yieldIfNotNull

internal object KDocReferenceResolver {
    /**
     * [symbol] is the symbol referenced by this resolve result.
     *
     * [receiverClassReference] is an optional receiver type in
     * the case of extension function references (see [getTypeQualifiedExtensions]).
     */
    private data class ResolveResult(val symbol: KaSymbol, val receiverClassReference: KaClassLikeSymbol?)

    private fun KaSymbol.toResolveResult(receiverClassReference: KaClassLikeSymbol? = null): ResolveResult =
        ResolveResult(symbol = this, receiverClassReference)

    private fun Iterable<KaSymbol>.toResolveResults(receiverClassReference: KaClassLikeSymbol? = null): List<ResolveResult> =
        this.map { it.toResolveResult(receiverClassReference) }

    context(_: KaSession)
    private fun <T : KaSymbol> Iterable<T>.getNonHiddenDeclarations(): List<T> =
        this.filter { it.deprecationStatus?.deprecationLevel != DeprecationLevelValue.HIDDEN }

    context(_: KaSession)
    private fun <T : KaSymbol> Sequence<T>.getNonHiddenDeclarations(): Sequence<T> =
        this.filter { it.deprecationStatus?.deprecationLevel != DeprecationLevelValue.HIDDEN }

    /**
     * Resolves the [selectedFqName] of KDoc
     *
     * To properly resolve qualifier parts in the middle,
     * we need to resolve the whole qualifier to understand which parts of the qualifier are package or class qualifiers.
     * And then we will be able to resolve the qualifier selected by the user to the proper class, package or callable.
     *
     * It's possible that the whole qualifier is invalid, in this case we still want to resolve our [selectedFqName].
     * To do this, we are trying to resolve the whole qualifier until we succeed.
     *
     * @param selectedFqName the selected fully qualified name of the KDoc
     * @param fullFqName the whole fully qualified name of the KDoc
     * @param contextElement the context element in which the KDoc is defined
     *
     * @return the set of [KaSymbol](s) resolved from the fully qualified name
     *         based on the selected FqName and context element
     */
    internal fun resolveKdocFqName(
        analysisSession: KaSession,
        selectedFqName: FqName,
        fullFqName: FqName,
        contextElement: KtElement,
    ): Set<KaSymbol> {
        with(analysisSession) {
            //ensure file context is provided for "non-physical" code as well
            val contextDeclarationOrSelf = PsiTreeUtil.getContextOfType(contextElement, KtDeclaration::class.java, false)
                ?: contextElement
            val fullSymbolsResolved =
                resolveKdocFqName(fullFqName, contextDeclarationOrSelf)
            if (selectedFqName == fullFqName) return fullSymbolsResolved.mapTo(mutableSetOf()) { it.symbol }
            if (fullSymbolsResolved.isEmpty()) {
                val parentFqName = fullFqName.parent()
                return resolveKdocFqName(analysisSession, selectedFqName, fullFqName = parentFqName, contextDeclarationOrSelf)
            }
            val goBackSteps = fullFqName.pathSegments().size - selectedFqName.pathSegments().size
            check(goBackSteps > 0) {
                "Selected FqName ($selectedFqName) should be smaller than the whole FqName ($fullFqName)"
            }
            return fullSymbolsResolved.mapNotNullTo(mutableSetOf()) { findParentSymbol(it, goBackSteps, selectedFqName) }
        }
    }

    /**
     * Finds the parent symbol of the given [ResolveResult] by traversing back up the symbol hierarchy a [goBackSteps] steps,
     * or until the containing class or object symbol is found.
     *
     * Knows about the [ResolveResult.receiverClassReference] field and uses it in case it's not empty.
     */
    private fun KaSession.findParentSymbol(resolveResult: ResolveResult, goBackSteps: Int, selectedFqName: FqName): KaSymbol? {
        return if (resolveResult.receiverClassReference != null) {
            findParentSymbol(resolveResult.receiverClassReference, goBackSteps - 1, selectedFqName)
        } else {
            findParentSymbol(resolveResult.symbol, goBackSteps, selectedFqName)
        }
    }

    /**
     * Finds the parent symbol of the given [KaSymbol] by traversing back up the symbol hierarchy a certain number of steps,
     * or until the containing class or object symbol is found.
     *
     * @param symbol The [KaSymbol] whose parent symbol needs to be found.
     * @param goBackSteps The number of steps to go back up the symbol hierarchy.
     * @param selectedFqName The fully qualified name of the selected package.
     * @return The [goBackSteps]-th parent [KaSymbol]
     */
    private fun KaSession.findParentSymbol(symbol: KaSymbol, goBackSteps: Int, selectedFqName: FqName): KaSymbol? {
        if (symbol !is KaDeclarationSymbol && symbol !is KaPackageSymbol) return null

        if (symbol is KaDeclarationSymbol) {
            goToNthParent(symbol, goBackSteps)?.let { return it }
        }

        return findPackage(selectedFqName)
    }

    /**
     * N.B. Works only for [KaClassSymbol] parents chain.
     */
    private fun KaSession.goToNthParent(symbol: KaDeclarationSymbol, steps: Int): KaDeclarationSymbol? {
        var currentSymbol = symbol

        repeat(steps) {
            currentSymbol = currentSymbol.containingDeclaration as? KaClassSymbol ?: return null
        }

        return currentSymbol
    }

    /**
     * Uses step-by-step algorithm to retrieve symbols with the given [fqName] from all the sources based on the given [contextElement].
     * If any symbols are found on some processing stage, they are returned immediately.
     *
     * Firstly, if the provided [fqName] is a short name, the function tries to acquire context declarations, i.e.,
     * declarations that are located inside the [contextElement]. Such declarations include: `this` references,
     * the context declaration itself, type/value parameters of this context declaration, etc.
     *
     * Then the algorithm prepares the sequence of scopes that might contain the short name.
     * Firstly, this sequence calculates a list of visible scopes: from local to global.
     * This list is a combination of scopes of all outer classes for this position
     * and scopes acquired from [ContextCollector][org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector].
     * Additionally, for cases when the referred symbol is not local and not imported,
     * the algorithm calculates the longest existing package name by [fqName] prefixes and adds the scope of this package to the scope sequence.
     *
     * After calculating all the scopes, the resolver starts processing each of these scopes one by one.
     * In every scope, the resolver looks for various declarations in the following order (from higher priority to lower):
     * 1. Classifiers
     * 2. Functions
     * 3. Synthetic properties
     * 4. Variables
     * If any symbols are found during this stage, the algorithm returns a set of symbols of the same declaration kind found in the same scope.
     *
     * Then, if no symbols were found on previous stages, the following symbol categories are searched:
     * 1. Type qualified extensions
     * 2. Packages
     * 3. Symbols provided via [AdditionalKDocResolutionProvider] extension point
     *
     * This resolution algorithm is implemented according to KEEP-0389 "Streamline ambiguous KDoc links".
     */
    private fun KaSession.resolveKdocFqName(
        fqName: FqName,
        contextElement: KtElement
    ): List<ResolveResult> {
        handleContextDeclarations(fqName, contextElement)?.let { return it }

        val visibleResolutionScopes = getVisibleScopes(contextElement)

        return findSymbolsInScopes(fqName, contextElement, visibleResolutionScopes)
    }

    /**
     * Retrieves symbols from the [contextElement] by [fqName].
     * Note that this search is performed only when [fqName] is short one, e.g., contains just a single segment.
     */
    private fun KaSession.handleContextDeclarations(
        fqName: FqName,
        contextElement: KtElement
    ): List<ResolveResult>? {
        if (fqName.isOneSegmentFQN()) {
            val shortName = fqName.shortName()

            // Search for symbols by `this` qualifier
            getExtensionReceiverSymbolByThisQualifier(shortName, contextElement).ifNotEmpty { return this.toResolveResults() }

            // Search for symbols from the context declaration
            getSymbolsFromDeclaration(
                shortName,
                contextElement,
            ).getNonHiddenDeclarations().ifNotEmpty { return this.toResolveResults() }
        }

        return null
    }

    /**
     * If [name] equals to `this`, returns a receiver of the callable context declaration, if any.
     */
    private fun KaSession.getExtensionReceiverSymbolByThisQualifier(
        name: Name,
        contextElement: KtElement,
    ): List<KaSymbol> {
        val owner = contextElement.parentOfType<KtDeclaration>(withSelf = true) ?: return emptyList()
        if (name.asString() == KtTokens.THIS_KEYWORD.value) {
            if (owner is KtCallableDeclaration && owner.receiverTypeReference != null) {
                val symbol = owner.symbol as? KaCallableSymbol ?: return emptyList()
                return listOfNotNull(symbol.receiverParameter)
            }
        }
        return emptyList()
    }

    private fun KaSession.getSymbolsFromDeclaration(name: Name, owner: KtElement): List<KaSymbol> = buildList {
        if (owner is KtNamedDeclaration) {
            if (owner.nameAsName == name) {
                add(owner.symbol)
            }
        }

        (owner as? KtModifierListOwner)?.modifierList?.contextParameterLists?.flatMap { it.contextParameters }
            ?.forEach { contextParameter ->
                if (contextParameter.nameAsName == name) {
                    add(contextParameter.symbol)
                }
            }

        if (owner is KtTypeParameterListOwner) {
            for (typeParameter in owner.typeParameters) {
                if (typeParameter.nameAsName == name) {
                    add(typeParameter.symbol)
                }
            }
        }

        if (owner is KtCallableDeclaration) {
            for (typeParameter in owner.valueParameters) {
                if (typeParameter.nameAsName == name) {
                    add(typeParameter.symbol)
                }
            }
        }

        if (owner is KtClassOrObject) {
            owner.primaryConstructor?.let { addAll(getSymbolsFromDeclaration(name, it)) }
        }
    }

    /**
     * Returns all scopes that are visible from the given [position] for the further declaration retrieval.
     */
    private fun KaSession.getVisibleScopes(
        position: KtElement
    ): List<KaScope> {
        val containingFile = position.containingKtFile
        val scopeContextScopes = containingFile.scopeContext(position).scopes
        val outerClassScopes = getOuterClassScopesForPosition(position)

        val localScopeContextScopes = scopeContextScopes.filter { scope ->
            scope.kind is KaScopeKind.LocalScope ||
                    scope.kind is KaScopeKind.TypeScope ||
                    scope.kind is KaScopeKind.TypeParameterScope ||
                    scope.kind is KaScopeKind.StaticMemberScope
        }.map { it.scope }

        val globalScopeContextScopes = scopeContextScopes.filter { scope ->
            scope.kind is KaScopeKind.PackageMemberScope ||
                    scope.kind is KaScopeKind.ImportingScope
        }.map { it.scope }

        return localScopeContextScopes + outerClassScopes + globalScopeContextScopes
    }

    private fun KaSession.findSymbolsInScopes(
        fqName: FqName,
        contextElement: KtElement,
        visibleResolutionScopes: List<KaScope>
    ): List<ResolveResult> {
        val shortName = fqName.shortName()

        val allScopesPossiblyContainingName = sequence {
            yieldAll(applyScopeReduction(fqName, visibleResolutionScopes))
            yieldIfNotNull(getLongestExistingPackageScope(fqName))
        }

        allScopesPossiblyContainingName.forEach { currentScope ->
            // Search for classifiers
            currentScope.classifiers(shortName).toSet().getNonHiddenDeclarations().ifNotEmpty { return this.toResolveResults() }

            val allCallables = currentScope.callables(shortName).toSet().getNonHiddenDeclarations()

            // Search for functions
            allCallables.filterIsInstance<KaFunctionSymbol>().ifNotEmpty { return this.toResolveResults() }

            // Search for synthetic properties
            getSymbolsFromSyntheticProperty(
                shortName,
                currentScope
            ).getNonHiddenDeclarations().ifNotEmpty { return this.toResolveResults() }

            // Search for variables
            allCallables.filterIsInstance<KaVariableSymbol>().ifNotEmpty { return this.toResolveResults() }
        }

        // Search for extension functions
        getTypeQualifiedExtensions(
            fqName,
            visibleResolutionScopes
        ).ifNotEmpty { return this }

        // Search for package
        findPackage(fqName)?.let {
            return listOf(it.toResolveResult())
        }

        // Search for symbols provided via `AdditionalKDocResolutionProvider` extension point
        AdditionalKDocResolutionProvider.resolveKdocFqName(useSiteSession, fqName, contextElement)
            .ifNotEmpty { return this.toResolveResults() }

        return emptyList()
    }

    /**
     * Calculates the longest existing package name for the given [fqName] and then performs scope reduction algorithm.
     * This is used to retrieve non-imported symbols from other packages by their fully qualified names.
     *
     * ```kotlin
     *  package foo.bar
     *
     *  object Something {
     *      fun myFun() {} // foo.bar.Something.myFun
     *  }
     * ```
     *
     * To find the package containing `foo.bar.Something.myFun`, this name is passed to [getLongestExistingPackageScope],
     * which then returns `foo.bar` package scope.
     * Then [getNestedScopePossiblyContainingShortName] is called for the rest of the [fqName] chain starting from the package scope.
     */
    private fun KaSession.getLongestExistingPackageScope(fqName: FqName): KaScope? {
        val fqNameSegments = fqName.pathSegments().map { it.toString() }
        for (numberOfPackageSegments in fqNameSegments.size - 1 downTo 1) {
            val packageName = FqName.fromSegments(fqNameSegments.take(numberOfPackageSegments))
            val declarationNameToFind = FqName.fromSegments(fqNameSegments.drop(numberOfPackageSegments))
            val packageScope = findPackage(packageName)?.packageScope ?: continue
            getNestedScopePossiblyContainingShortName(declarationNameToFind, packageScope)?.let { return it }
        }
        return null
    }

    /**
     * Generates various possible getter names for the given [name] and searches for symbols in the given [scope].
     */
    private fun getSymbolsFromSyntheticProperty(name: Name, scope: KaScope): List<KaSymbol> {
        val getterNames = possibleGetMethodNames(name)
        return scope.callables { it in getterNames }.filterIsInstance<KaFunctionSymbol>().filter { symbol ->
            val symbolLocation = symbol.location
            val symbolOrigin = symbol.origin
            val parametersCount = symbol.valueParameters.size
            parametersCount == 0 && symbolLocation == KaSymbolLocation.CLASS &&
                    (symbolOrigin == KaSymbolOrigin.JAVA_LIBRARY || symbolOrigin == KaSymbolOrigin.JAVA_SOURCE)
        }.toList()
    }

    /**
     * Retrieves all outer declarations for [contextElement] and returns a list of their member scopes.
     */
    private fun KaSession.getOuterClassScopesForPosition(contextElement: KtElement): Collection<KaScope> {
        val declaration = PsiTreeUtil.getContextOfType(contextElement, KtDeclaration::class.java, false)
            ?: return emptyList()
        val scopeList = mutableListOf<KaScope>()

        for (ktDeclaration in declaration.parentsOfType<KtDeclaration>(withSelf = true)) {
            if (ktDeclaration is KtClassOrObject) {
                val symbol = ktDeclaration.classSymbol ?: continue

                val scope = getCompositeCombinedMemberAndCompanionObjectScope(symbol)

                scopeList.add(scope)
            }
        }
        return scopeList
    }

    private fun KaSession.getCompositeCombinedMemberAndCompanionObjectScope(symbol: KaDeclarationContainerSymbol): KaScope =
        listOfNotNull(
            symbol.combinedMemberScope,
            getCompanionObjectMemberScope(symbol),
        ).asCompositeScope()

    private fun KaSession.getCompanionObjectMemberScope(symbol: KaDeclarationContainerSymbol): KaScope? {
        val namedClassSymbol = symbol as? KaNamedClassSymbol ?: return null
        val companionSymbol = namedClassSymbol.companionObject ?: return null
        return companionSymbol.memberScope
    }

    /**
     * Takes [scope] and [fqName] to look for, returns the final scope that might contain the short name of [fqName]
     * or `null` when the required scope wasn't found.
     *
     * Does it by iterating over [fqName] segments (excluding the last one) and searching for declaration containers
     * with the current segment's name in the current scope. When suitable declaration containers for the current segment are calculated,
     * proceeds with a composite member scope of these containers and the next [fqName] segment.
     */
    private fun KaSession.getNestedScopePossiblyContainingShortName(fqName: FqName, scope: KaScope): KaScope? {
        if (fqName.isOneSegmentFQN()) {
            return scope
        }

        val finalScope = fqName.pathSegments()
            .dropLast(1)
            .fold(scope) { currentScope, fqNamePart ->
                val currentClassifiers = currentScope
                    .classifiers(fqNamePart).filterIsInstance<KaDeclarationContainerSymbol>()

                if (currentClassifiers.none()) return null

                currentClassifiers
                    .map { getCompositeCombinedMemberAndCompanionObjectScope(it) }
                    .toList()
                    .asCompositeScope()
            }

        return finalScope
    }

    private fun KaSession.applyScopeReduction(fqName: FqName, scopes: List<KaScope>): List<KaScope> {
        return scopes.mapNotNull { getNestedScopePossiblyContainingShortName(fqName, it) }
    }

    /**
     * Tries to resolve [fqName] into available extension callables (functions or properties)
     * prefixed with a suitable extension receiver type (like in `Foo.bar`, or `foo.Foo.bar`).
     *
     * Relies on the fact that in such references only the last qualifier refers to the
     * actual extension callable, and the part before that refers to the receiver type (either fully
     * or partially qualified).
     *
     * For example, `foo.Foo.bar` may only refer to the extension callable `bar` with
     * a `foo.Foo` receiver type, and this function will only look for such combinations.
     *
     * N.B. This function only searches for extension callables qualified by receiver types!
     * It does not try to resolve fully qualified or member functions, because they are dealt
     * with by the other parts of [KDocReferenceResolver].
     */
    private fun KaSession.getTypeQualifiedExtensions(
        fqName: FqName,
        visibleScopes: List<KaScope>,
    ): List<ResolveResult> {
        if (fqName.isRoot) return emptyList()
        val extensionName = fqName.shortName()

        val receiverTypeName = fqName.parent()
        if (receiverTypeName.isRoot) return emptyList()

        val scopesContainingPossibleReceivers = sequence {
            yieldAll(applyScopeReduction(receiverTypeName, visibleScopes))
            yieldIfNotNull(getLongestExistingPackageScope(receiverTypeName))
        }

        val possibleReceivers =
            scopesContainingPossibleReceivers.flatMap { it.classifiers(receiverTypeName.shortName()) }
                .filterIsInstance<KaClassLikeSymbol>().toSet().getNonHiddenDeclarations()
        val possibleExtensionsByScope =
            visibleScopes.map { scope ->
                scope.callables(extensionName)
                    .filter { it.isExtension }.toSet().getNonHiddenDeclarations()
            }

        if (possibleExtensionsByScope.flatten().isEmpty() || possibleReceivers.isEmpty()) return emptyList()

        return possibleReceivers.mapNotNull { receiverClassSymbol ->
            val receiverType = receiverClassSymbol.defaultType
            possibleExtensionsByScope.firstNotNullOfOrNull { extensions ->
                extensions.filter { it.canBeCalledAsExtensionOn(receiverType) }
                    .toResolveResults(receiverClassReference = receiverClassSymbol).ifEmpty { null }
            }
        }.flatten()
    }
}