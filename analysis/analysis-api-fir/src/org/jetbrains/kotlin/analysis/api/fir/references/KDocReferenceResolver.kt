/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.getContextElementOrSelf
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.getLongestExistingPackageScope
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.getTypeQualifiedExtensions
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver.resolveKdocFqName
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.load.java.possibleGetMethodNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.references.utils.KotlinKDocResolutionStrategyProviderService
import org.jetbrains.kotlin.utils.addIfNotNull
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
     * @param containedTagSectionIfSubject the containing KDoc tag section if the link is the subject of this section (e.g., `@param` or `@property`).
     *
     * @return the set of [KaSymbol](s) resolved from the fully qualified name
     *         based on the selected FqName and context element
     */
    internal fun resolveKdocFqName(
        analysisSession: KaSession,
        selectedFqName: FqName,
        fullFqName: FqName,
        contextElement: KtElement,
        containedTagSectionIfSubject: KDocKnownTag?
    ): Set<KaSymbol> {
        with(analysisSession) {
            if (KotlinKDocResolutionStrategyProviderService
                    .getService(useSiteModule.project)
                    ?.shouldUseExperimentalStrategy() != true
            ) {
                return ClassicKDocReferenceResolver.resolveKdocFqName(
                    analysisSession,
                    selectedFqName,
                    fullFqName,
                    contextElement
                ).toSet()
            }

            val contextDeclarationOrSelf = contextElement.getContextElementOrSelf()
            val fullSymbolsResolved =
                resolveKdocFqName(fullFqName, contextDeclarationOrSelf, containedTagSectionIfSubject)
            if (selectedFqName == fullFqName) {
                val resolvedSymbols = fullSymbolsResolved.map { it.symbol }
                return when (containedTagSectionIfSubject) {
                    KDocKnownTag.THROWS, KDocKnownTag.EXCEPTION -> resolvedSymbols.filterIsInstance<KaClassLikeSymbol>().filter { symbol ->
                        symbol.defaultType.isSubtypeOf(analysisSession.builtinTypes.throwable)
                    }
                    else -> resolvedSymbols
                }.toSet()
            }
            if (fullSymbolsResolved.none()) {
                val parent = fullFqName.parent()
                return resolveKdocFqName(analysisSession, selectedFqName, parent, contextDeclarationOrSelf, null)
            }
            val goBackSteps = fullFqName.pathSegments().size - selectedFqName.pathSegments().size
            check(goBackSteps > 0) {
                "Selected FqName ($selectedFqName) should be smaller than the whole FqName ($fullFqName)"
            }
            return fullSymbolsResolved.mapNotNull { findParentSymbol(it, goBackSteps, selectedFqName) }.toSet()
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
     * Adjusts the position the context is collected for, returns the first non-KDoc context element found.
     * It's required to properly handle self-links, as all self-link declarations are retrieved from the context element.
     * However, we couldn't just take the first [KtDeclaration] parent of some KDoc element, because it simply breaks the resolution for dangling references.
     *
     * ```kotlin
     * fun foo(t: String) {
     *     val foo = "hi"
     *
     *     /**
     *      * [foo] is a local variable!
     *      */
     * }
     * ```
     *
     * The first [KtDeclaration] parent of the KDoc element is `foo` function, however, the reference itself is contained in the function scope and points to a local variable.
     * With `foo` function being the context element, [org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.scopeContext] returns the context for the function declaration,
     * which doesn't include the local variable. But [getContextElementOrSelf] actually returns [KtBlockExpression] of the function, for which the scope context collects all local declarations.
     */
    private fun KtElement.getContextElementOrSelf(): KtElement {
        return PsiTreeUtil.findFirstContext(this, false) { context ->
            context !is KDocElement && context !is KDocName && context !is KDocLink
        } as? KtElement ?: this
    }

    /**
     * Uses step-by-step algorithm to retrieve symbols with the given [fqName] from all the sources based on the given [contextElement].
     * If any symbols are found on some processing stage, they are returned immediately.
     *
     * Firstly, if the provided [fqName] is a short name, the function tries to acquire self-declarations, i.e., declarations that are located inside the [contextElement].
     * Such declarations include: `this` references, the name of the context declaration, type/value parameters of this context declaration, etc.
     *
     * Then the algorithm prepares the sequence of scopes that might contain the short name.
     * Firstly, this sequence includes lexical scopes of the [contextElement] and its parents, i.e., a sequence of member scopes from outer classes.
     * Then [resolveKdocFqName] uses [org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector] to collect scopes accessible from the given [contextElement],
     * which is done via [org.jetbrains.kotlin.analysis.api.components.KaScopeProvider.scopeContext].
     * The scope collector returns accessible local and imported scopes sorted by their locality (starting from the most local ones), see all types of provided scopes in [org.jetbrains.kotlin.analysis.api.components.KaScopeKinds].
     * Additionally, for cases when the referred symbol is not local and not imported, the algorithm calculates the longest existing package name by [fqName] prefixes and adds the scope of this package to the scope sequence.
     *
     * Symbols from these scopes are retrieved based on the following priorities:
     * 1. Classifiers
     * 2. Functions
     * 3. Synthetic properties
     * 4. Variables
     * If any symbols are found during this stage, the algorithm returns a set of symbols of the same declaration kind found in the same scope.
     *
     * Then, if no symbols were found on previous stages, the following symbol categories are searched:
     * 1. Type qualified extensions
     * 2. Package
     * 3. Symbols provided via [AdditionalKDocResolutionProvider] extension point
     */
    private fun KaSession.resolveKdocFqName(
        fqName: FqName,
        contextElement: KtElement,
        containedTagSectionIfSubject: KDocKnownTag?
    ): List<ResolveResult> {
        val shortName = fqName.shortName()
        if (fqName.isOneSegmentFQN()) {
            if (containedTagSectionIfSubject == KDocKnownTag.PROPERTY) {
                val containingClass = contextElement as? KtClassOrObject ?: return emptyList()
                val propertySymbol = containingClass.classSymbol?.declaredMemberScope?.callables?.firstOrNull { callable ->
                    callable is KaPropertySymbol && callable.name == shortName
                }

                return listOfNotNull(propertySymbol).toResolveResults()
            }

            // Search for symbols from the context declaration (self-links)
            val symbolsFromSelf = getSymbolsFromContextDeclaration(
                shortName,
                contextElement,
                shouldCollectPropertyParameters = containedTagSectionIfSubject == KDocKnownTag.PARAM,
            )

            when (containedTagSectionIfSubject) {
                KDocKnownTag.PARAM -> return symbolsFromSelf.filter { symbol ->
                    symbol is KaValueParameterSymbol ||
                            symbol is KaContextParameterSymbol ||
                            symbol is KaTypeParameterSymbol
                }.toResolveResults()
                else -> symbolsFromSelf.ifNotEmpty { return this.toResolveResults() }
            }

            // Search for symbols by `this` qualifier
            getExtensionReceiverSymbolByThisQualifier(shortName, contextElement).ifNotEmpty { return this.toResolveResults() }
        }
        val containingFile = contextElement.containingKtFile
        val scopeContext = containingFile.scopeContext(contextElement)
        val scopeContextScopes = scopeContext.scopes
        val lexicalScopes = getLexicalScopesForPosition(contextElement)

        val localScopeContextScopes = scopeContext.scopes.filter { scope ->
            scope.kind is KaScopeKind.LocalScope ||
                    scope.kind is KaScopeKind.TypeScope ||
                    scope.kind is KaScopeKind.TypeParameterScope ||
                    scope.kind is KaScopeKind.StaticMemberScope
        }.map { it.scope }

        val globalScopeContextScopes = scopeContext.scopes.filter { scope ->
            scope.kind is KaScopeKind.PackageMemberScope ||
                    scope.kind is KaScopeKind.ImportingScope
        }.map { it.scope }

        val visibleResolutionScopes = localScopeContextScopes + lexicalScopes + globalScopeContextScopes

        val allScopesPossiblyContainingName = sequence {
            yieldAll(visibleResolutionScopes.mapNotNull { getNestedScopePossiblyContainingShortName(fqName, it) })
            yieldIfNotNull(getLongestExistingPackageScope(fqName))
        }

        allScopesPossiblyContainingName.firstNotNullOfOrNull { currentScope ->
            // Search for classifiers
            currentScope.classifiers(shortName).toSet().ifNotEmpty { return this.toResolveResults() }

            val allCallables = currentScope.callables(shortName)

            // Search for functions
            allCallables.filterIsInstance<KaFunctionSymbol>().toSet().ifNotEmpty { return this.toResolveResults() }

            // Search for synthetic properties
            getSymbolsFromSyntheticProperty(
                shortName,
                currentScope
            ).ifNotEmpty { return this.toResolveResults() }

            // Search for variables
            allCallables.filterIsInstance<KaVariableSymbol>().toSet().ifNotEmpty { return this.toResolveResults() }
        }

        // Search for extension functions
        getTypeQualifiedExtensions(
            fqName,
            scopeContextScopes.map { it.scope },
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
     * Calculates the longest existing package name for the given [fqName] by prefixes and returns its scope.
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
     * To find the package containing `foo.bar.Something.myFun`, this name is passed to [getLongestExistingPackageScope], which then returns `foo.bar` package scope.
     */
    private fun KaSession.getLongestExistingPackageScope(fqName: FqName): KaScope? {
        val fqNameSegments = fqName.pathSegments()
        for (numberOfPackageSegments in fqNameSegments.size - 1 downTo 1) {
            val packageName = FqName.fromSegments(fqNameSegments.take(numberOfPackageSegments).map { it.toString() })
            val declarationNameToFind = FqName.fromSegments(fqNameSegments.drop(numberOfPackageSegments).map { it.toString() })
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
        return scope.callables { it in getterNames }.filter { symbol ->
            val symbolLocation = symbol.location
            val symbolOrigin = symbol.origin
            symbolLocation == KaSymbolLocation.CLASS && (symbolOrigin == KaSymbolOrigin.JAVA_LIBRARY || symbolOrigin == KaSymbolOrigin.JAVA_SOURCE)
        }.toList()
    }

    /**
     * If [name] equals to `this`, returns a receiver of the callable context declaration, if any.
     */
    private fun KaSession.getExtensionReceiverSymbolByThisQualifier(
        name: Name,
        contextElement: KtElement,
    ): List<KaSymbol> {
        val owner = contextElement.parentOfType<KtDeclaration>(withSelf = true) ?: return emptyList()
        if (name.asString() == "this") {
            if (owner is KtCallableDeclaration && owner.receiverTypeReference != null) {
                val symbol = owner.symbol as? KaCallableSymbol ?: return emptyList()
                return listOfNotNull(symbol.receiverParameter)
            }
        }
        return emptyList()
    }

    /**
     * Retrieves suitable symbols from [contextElement].
     *
     * See this [KEEP on self-links](https://github.com/Kotlin/KEEP/blob/kdoc/Streamline-KDoc-ambiguity-references/proposals/kdoc/streamline-KDoc-ambiguity-references.md#self-links)
     * for more details.
     */
    private fun KaSession.getSymbolsFromContextDeclaration(
        name: Name,
        contextElement: KtElement,
        shouldCollectPropertyParameters: Boolean,
    ): List<KaSymbol> = buildList {
        if (contextElement !is KtDeclaration) {
            return@buildList
        }

        fun collectParametersAndProperties(declaration: KtDeclaration) {
            if (declaration is KtCallableDeclaration) {
                for (valueParameter in declaration.valueParameters) {
                    val valueParameterName = valueParameter.nameAsName

                    if (valueParameterName != name) {
                        continue
                    }

                    when (declaration) {
                        is KtPrimaryConstructor -> {
                            val classParameterProperty =
                                declaration.containingClass()?.classSymbol?.declaredMemberScope?.callables?.firstOrNull { callable ->
                                    callable is KaPropertySymbol && callable.isFromPrimaryConstructor && callable.name == name
                                }
                            addIfNotNull(classParameterProperty)

                            if (classParameterProperty == null || shouldCollectPropertyParameters) {
                                add(valueParameter.symbol)
                            }
                        }
                        else -> add(valueParameter.symbol)
                    }
                }
            }

            declaration.modifierList?.contextReceiverLists?.flatMap { it.contextParameters() }?.forEach { contextParameter ->
                if (contextParameter.nameAsName == name) {
                    add(contextParameter.symbol)
                }
            }

            if (declaration is KtTypeParameterListOwner) {
                for (typeParameter in declaration.typeParameters) {
                    if (typeParameter.nameAsName == name) {
                        add(typeParameter.symbol)
                    }
                }
            }
        }

        if ((contextElement as? KtNamedDeclaration)?.nameAsName == name) {
            add(contextElement.symbol)
        }

        collectParametersAndProperties(contextElement)

        if (contextElement is KtClassOrObject) {
            val primaryConstructor = contextElement.primaryConstructor ?: return@buildList

            if (contextElement.nameAsName == name) {
                addIfNotNull(primaryConstructor.symbol)
            }

            collectParametersAndProperties(primaryConstructor)
        }
    }

    /**
     * Retrieves all outer declarations for [contextElement] and returns a list of their member scopes.
     */
    private fun KaSession.getLexicalScopesForPosition(contextElement: KtElement): Collection<KaScope> {
        val declaration = PsiTreeUtil.getContextOfType(contextElement, KtDeclaration::class.java, false) ?: return emptyList()
        val scopeList = mutableListOf<KaScope>()

        // This is also workaround for KT-79653 allowing pointing to enum entries of the context enum class
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
     * Takes [scope] and [fqName] to look for, returns the final scope that might contain the short name of [fqName] or `null` when the required scope wasn't found.
     *
     * Does it by iterating over [fqName] segments (excluding the last one) and searching for declaration containers with the current segment's name in the current scope.
     * When suitable declaration containers for the current segment are calculated, proceeds with a composite member scope of these containers and the next [fqName] segment.
     */
    private fun KaSession.getNestedScopePossiblyContainingShortName(fqName: FqName, scope: KaScope): KaScope? {
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
        potentialReceiverScopes: List<KaScope>,
        potentialExtensionScopes: List<KaScope>,
    ): List<ResolveResult> {
        if (fqName.isRoot) return emptyList()
        val extensionName = fqName.shortName()

        val receiverTypeName = fqName.parent()
        if (receiverTypeName.isRoot) return emptyList()

        val scopesContainingPossibleReceivers = sequence {
            yieldAll(potentialReceiverScopes.mapNotNull { getNestedScopePossiblyContainingShortName(receiverTypeName, it) })
            yieldIfNotNull(getLongestExistingPackageScope(receiverTypeName))
        }

        val possibleReceivers =
            scopesContainingPossibleReceivers.flatMap { it.classifiers(receiverTypeName.shortName()) }.filterIsInstance<KaClassLikeSymbol>()
                .toSet()
        val possibleExtensionsByScope =
            potentialExtensionScopes.map { scope -> scope.callables(extensionName).filter { it.isExtension }.toSet() }

        if (possibleExtensionsByScope.flatten().none() || possibleReceivers.none()) return emptyList()

        return possibleReceivers.mapNotNull { receiverClassSymbol ->
            val receiverType = receiverClassSymbol.defaultType
            possibleExtensionsByScope.firstNotNullOfOrNull { extensions ->
                extensions.filter { it.canBeCalledAsExtensionOn(receiverType) }
                    .toResolveResults(receiverClassReference = receiverClassSymbol).ifEmpty { null }
            }
        }.flatten()
    }
}