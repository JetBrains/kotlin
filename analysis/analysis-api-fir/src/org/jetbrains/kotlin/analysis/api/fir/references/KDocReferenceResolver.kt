/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.load.java.possibleGetMethodNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import kotlin.reflect.KClass

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
     * @return the collection of [KaSymbol](s) resolved from the fully qualified name
     *         based on the selected FqName and context element
     */
    internal fun resolveKdocFqName(
        analysisSession: KaSession,
        selectedFqName: FqName,
        fullFqName: FqName,
        contextElement: KtElement,
    ): Collection<KaSymbol> {
        with(analysisSession) {
            //ensure file context is provided for "non-physical" code as well
            val contextDeclarationOrSelf = PsiTreeUtil.getContextOfType(contextElement, KtDeclaration::class.java, false)
                ?: contextElement
            val fullSymbolsResolved = resolveKdocFqName(fullFqName, contextDeclarationOrSelf)
            if (selectedFqName == fullFqName) return fullSymbolsResolved.mapTo(mutableSetOf()) { it.symbol }
            if (fullSymbolsResolved.isEmpty()) {
                val parent = fullFqName.parent()
                return resolveKdocFqName(analysisSession, selectedFqName, parent, contextDeclarationOrSelf)
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

    private fun KaSession.resolveKdocFqName(
        fqName: FqName,
        contextElement: KtElement,
        trySyntheticGetters: Boolean = true,
    ): Collection<ResolveResult> {
        getExtensionReceiverSymbolByThisQualifier(fqName, contextElement).ifNotEmpty { return this }

        buildList {
            getSymbolsFromScopes(fqName, contextElement).mapTo(this) { it.toResolveResult() }
            addAll(getTypeQualifiedExtensions(fqName, contextElement))
            addIfNotNull(findPackage(fqName)?.toResolveResult())
        }.ifNotEmpty { return this }

        getNonImportedSymbolsByFullyQualifiedName(fqName).map { it.toResolveResult() }.ifNotEmpty { return this }

        AdditionalKDocResolutionProvider.resolveKdocFqName(useSiteSession, fqName, contextElement)
            .map { it.toResolveResult() }
            .ifNotEmpty { return this }

        if (trySyntheticGetters) {
            getSymbolsFromSyntheticProperty(fqName, contextElement).ifNotEmpty { return this }
        }

        return emptyList()
    }

    private fun KaSession.getSymbolsFromSyntheticProperty(fqName: FqName, contextElement: KtElement): Collection<ResolveResult> {
        val getterNames = possibleGetMethodNames(fqName.shortNameOrSpecial())
        return getterNames.flatMap { getterName ->
            resolveKdocFqName(fqName.parent().child(getterName), contextElement, trySyntheticGetters = false)
        }
    }

    private fun KaSession.getExtensionReceiverSymbolByThisQualifier(
        fqName: FqName,
        contextElement: KtElement,
    ): Collection<ResolveResult> {
        val owner = contextElement.parentOfType<KtDeclaration>(withSelf = true) ?: return emptyList()
        if (fqName.pathSegments().singleOrNull()?.asString() == "this") {
            if (owner is KtCallableDeclaration && owner.receiverTypeReference != null) {
                val symbol = owner.symbol as? KaCallableSymbol ?: return emptyList()
                return listOfNotNull(symbol.receiverParameter?.toResolveResult())
            }
        }
        return emptyList()
    }

    private fun KaSession.getSymbolsFromScopes(fqName: FqName, contextElement: KtElement): Collection<KaSymbol> {
        getSymbolsFromParentMemberScopes(fqName, contextElement).ifNotEmpty { return this }
        val importScopeContext = contextElement.containingKtFile.importingScopeContext
        getSymbolsFromImportingScope(importScopeContext, fqName, KaScopeKind.ExplicitSimpleImportingScope::class).ifNotEmpty { return this }
        getSymbolsFromPackageScope(fqName, contextElement).ifNotEmpty { return this }
        getSymbolsFromImportingScope(importScopeContext, fqName, KaScopeKind.DefaultSimpleImportingScope::class).ifNotEmpty { return this }
        getSymbolsFromImportingScope(importScopeContext, fqName, KaScopeKind.ExplicitStarImportingScope::class).ifNotEmpty { return this }
        getSymbolsFromImportingScope(importScopeContext, fqName, KaScopeKind.DefaultStarImportingScope::class).ifNotEmpty { return this }
        return emptyList()
    }

    private fun KaSession.getSymbolsFromDeclaration(name: Name, owner: KtDeclaration): List<KaSymbol> = buildList {
        if (owner is KtNamedDeclaration) {
            if (owner.nameAsName == name) {
                add(owner.symbol)
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
     * Returns the [KaSymbol]s called [fqName] found in the member scope and companion object's member scope of the [KtDeclaration]s that
     * contain the [contextElement].
     *
     * If [fqName] has two or more segments, e.g. `Foo.bar`, the member and companion object scope of the containing [KtDeclaration] will be
     * queried for a class `Foo` first, and then that class `Foo` will be queried for the member `bar` by short name.
     */
    private fun KaSession.getSymbolsFromParentMemberScopes(fqName: FqName, contextElement: KtElement): Collection<KaSymbol> {
        val declaration = PsiTreeUtil.getContextOfType(contextElement, KtDeclaration::class.java, false) ?: return emptyList()
        for (ktDeclaration in declaration.parentsOfType<KtDeclaration>(withSelf = true)) {
            if (fqName.pathSegments().size == 1) {
                getSymbolsFromDeclaration(fqName.shortName(), ktDeclaration).ifNotEmpty { return this }
            }
            if (ktDeclaration is KtClassOrObject) {
                val symbol = ktDeclaration.classSymbol ?: continue

                val scope = getCompositeCombinedMemberAndCompanionObjectScope(symbol)

                val symbolsFromScope = getSymbolsFromMemberScope(fqName, scope)
                if (symbolsFromScope.isNotEmpty()) return symbolsFromScope
            }
        }
        return emptyList()
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

    private fun KaSession.getSymbolsFromPackageScope(fqName: FqName, contextElement: KtElement): Collection<KaDeclarationSymbol> {
        val containingFile = contextElement.containingKtFile
        val packageFqName = containingFile.packageFqName
        val packageSymbol = findPackage(packageFqName) ?: return emptyList()
        val packageScope = packageSymbol.packageScope
        return getSymbolsFromMemberScope(fqName, packageScope)
    }

    private fun KaSession.getSymbolsFromImportingScope(
        scopeContext: KaScopeContext,
        fqName: FqName,
        acceptScopeKind: KClass<out KaScopeKind>,
    ): Collection<KaDeclarationSymbol> {
        val importingScope = scopeContext.compositeScope { acceptScopeKind.java.isAssignableFrom(it::class.java) }
        return getSymbolsFromMemberScope(fqName, importingScope)
    }

    private fun KaSession.getSymbolsFromMemberScope(fqName: FqName, scope: KaScope): Collection<KaDeclarationSymbol> {
        val finalScope = fqName.pathSegments()
            .dropLast(1)
            .fold(scope) { currentScope, fqNamePart ->
                currentScope
                    .classifiers(fqNamePart)
                    .filterIsInstance<KaDeclarationContainerSymbol>()
                    .map { getCompositeCombinedMemberAndCompanionObjectScope(it) }
                    .toList()
                    .asCompositeScope()
            }

        return finalScope.getAllSymbolsFromScopeByShortName(fqName)
    }

    private fun KaScope.getAllSymbolsFromScopeByShortName(fqName: FqName): Collection<KaDeclarationSymbol> {
        val shortName = fqName.shortName()
        return buildSet {
            addAll(callables(shortName))
            addAll(classifiers(shortName))
        }
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
    private fun KaSession.getTypeQualifiedExtensions(fqName: FqName, contextElement: KtElement): Collection<ResolveResult> {
        if (fqName.isRoot) return emptyList()
        val extensionName = fqName.shortName()

        val receiverTypeName = fqName.parent()
        if (receiverTypeName.isRoot) return emptyList()

        val possibleExtensions = getExtensionCallableSymbolsByShortName(extensionName, contextElement)
        if (possibleExtensions.isEmpty()) return emptyList()

        val possibleReceivers = getReceiverTypeCandidates(receiverTypeName, contextElement)

        return possibleReceivers.flatMap { receiverClassSymbol ->
            val receiverType = buildClassType(receiverClassSymbol)
            val applicableExtensions = possibleExtensions.filter { canBeReferencedAsExtensionOn(it, receiverType) }

            applicableExtensions.map { it.toResolveResult(receiverClassReference = receiverClassSymbol) }
        }
    }

    private fun KaSession.getExtensionCallableSymbolsByShortName(name: Name, contextElement: KtElement): List<KaCallableSymbol> {
        return getSymbolsFromScopes(FqName.topLevel(name), contextElement)
            .filterIsInstance<KaCallableSymbol>()
            .filter { it.isExtension }
    }

    private fun KaSession.getReceiverTypeCandidates(receiverTypeName: FqName, contextElement: KtElement): List<KaClassLikeSymbol> {
        val possibleReceivers =
            getSymbolsFromScopes(receiverTypeName, contextElement).ifEmpty { null }
                ?: getNonImportedSymbolsByFullyQualifiedName(receiverTypeName).ifEmpty { null }
                ?: emptyList()

        return possibleReceivers.filterIsInstance<KaClassLikeSymbol>()
    }

    /**
     * Returns true if we consider that [this] extension function prefixed with [actualReceiverType] in
     * a KDoc reference should be considered as legal and resolved, and false otherwise.
     *
     * This is **not** an actual type check, it is just an opinionated approximation.
     * The main guideline was K1 KDoc resolve.
     *
     * This check might change in the future, as Dokka team advances with KDoc rules.
     */
    private fun KaSession.canBeReferencedAsExtensionOn(symbol: KaCallableSymbol, actualReceiverType: KaType): Boolean {
        val extensionReceiverType = symbol.receiverParameter?.returnType ?: return false
        return isPossiblySuperTypeOf(extensionReceiverType, actualReceiverType)
    }

    /**
     * Same constraints as in [canBeReferencedAsExtensionOn].
     *
     * For a similar function in the `intellij` repository, see `isPossiblySubTypeOf`.
     */
    private fun KaSession.isPossiblySuperTypeOf(type: KaType, actualReceiverType: KaType): Boolean {
        // Type parameters cannot act as receiver types in KDoc
        if (actualReceiverType is KaTypeParameterType) return false

        if (type is KaTypeParameterType) {
            return type.symbol.upperBounds.all { isPossiblySuperTypeOf(it, actualReceiverType) }
        }

        val receiverExpanded = actualReceiverType.expandedSymbol
        val expectedExpanded = type.expandedSymbol

        // if the underlying classes are equal, we consider the check successful
        // despite the possibility of different type bounds
        if (
            receiverExpanded != null &&
            receiverExpanded == expectedExpanded
        ) {
            return true
        }

        return actualReceiverType.isSubtypeOf(type)
    }

    private fun KaSession.getNonImportedSymbolsByFullyQualifiedName(fqName: FqName): Collection<KaSymbol> = buildSet {
        generateNameInterpretations(fqName).forEach { interpretation ->
            collectSymbolsByFqNameInterpretation(interpretation, this@buildSet)
        }
    }

    private fun KaSession.collectSymbolsByFqNameInterpretation(
        interpretation: FqNameInterpretation,
        consumer: MutableCollection<KaSymbol>,
    ) {
        when (interpretation) {
            is FqNameInterpretation.FqNameInterpretationAsCallableId -> {
                collectSymbolsByFqNameInterpretationAsCallableId(interpretation.callableId, consumer)
            }

            is FqNameInterpretation.FqNameInterpretationAsClassId -> {
                collectSymbolsByClassId(interpretation.classId, consumer)
            }

            is FqNameInterpretation.FqNameInterpretationAsPackage -> {
                collectSymbolsByPackage(interpretation.packageFqName, consumer)
            }
        }
    }

    private fun KaSession.collectSymbolsByPackage(packageFqName: FqName, consumer: MutableCollection<KaSymbol>) {
        val symbol = findPackage(packageFqName)
        consumer.addIfNotNull(symbol)
    }

    private fun KaSession.collectSymbolsByClassId(classId: ClassId, consumer: MutableCollection<KaSymbol>) {
        val symbol = findClass(classId) ?: findTypeAlias(classId)
        consumer.addIfNotNull(symbol)
    }

    private fun KaSession.collectSymbolsByFqNameInterpretationAsCallableId(
        callableId: CallableId,
        consumer: MutableCollection<KaSymbol>,
    ) {
        when (val classId = callableId.classId) {
            null -> {
                consumer.addAll(findTopLevelCallables(callableId.packageName, callableId.callableName))
            }

            else -> {
                val symbol = findClass(classId)
                if (symbol != null) {
                    val scope = getCompositeCombinedMemberAndCompanionObjectScope(symbol)
                    consumer.addAll(scope.callables(callableId.callableName))
                }
            }
        }
    }

    private fun generateNameInterpretations(fqName: FqName): Sequence<FqNameInterpretation> = sequence {
        val parts = fqName.pathSegments()
        if (parts.isEmpty()) {
            yield(FqNameInterpretation.create(packageParts = emptyList(), classParts = emptyList(), callable = null))
            return@sequence
        }
        for (lastPackagePartIndexExclusive in 0..parts.size) {
            yield(
                FqNameInterpretation.create(
                    packageParts = parts.subList(0, lastPackagePartIndexExclusive),
                    classParts = parts.subList(lastPackagePartIndexExclusive, parts.size),
                    callable = null,
                )
            )

            if (lastPackagePartIndexExclusive <= parts.size - 1) {
                yield(
                    FqNameInterpretation.create(
                        packageParts = parts.subList(0, lastPackagePartIndexExclusive),
                        classParts = parts.subList(lastPackagePartIndexExclusive, parts.size - 1),
                        callable = parts.last(),
                    )
                )
            }
        }
    }
}

private sealed class FqNameInterpretation {

    data class FqNameInterpretationAsPackage(val packageFqName: FqName) : FqNameInterpretation()
    data class FqNameInterpretationAsClassId(val classId: ClassId) : FqNameInterpretation()
    data class FqNameInterpretationAsCallableId(val callableId: CallableId) : FqNameInterpretation()

    companion object {
        fun create(packageParts: List<Name>, classParts: List<Name>, callable: Name?): FqNameInterpretation {
            val packageName = FqName.fromSegments(packageParts.map { it.asString() })
            val relativeClassName = FqName.fromSegments(classParts.map { it.asString() })

            return when {
                classParts.isEmpty() && callable == null -> FqNameInterpretationAsPackage(packageName)
                callable == null -> FqNameInterpretationAsClassId(ClassId(packageName, relativeClassName, isLocal = false))
                else -> FqNameInterpretationAsCallableId(CallableId(packageName, relativeClassName.takeUnless { it.isRoot }, callable))
            }
        }
    }
}
