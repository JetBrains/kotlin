/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeKind
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import kotlin.reflect.KClass
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol

internal object KDocReferenceResolver {

    context(KtAnalysisSession)
    internal fun resolveKdocFqName(fqName: FqName, contextElement: KtElement): Collection<KtSymbol> {
        getSymbolsFromParentMemberScopes(fqName, contextElement).ifNotEmpty { return this }
        val importScopeContext = contextElement.containingKtFile.getImportingScopeContext()
        getSymbolsFromImportingScope(importScopeContext, fqName, KtScopeKind.ExplicitSimpleImportingScope::class).ifNotEmpty { return this }
        getSymbolsFromPackageScope(fqName, contextElement).ifNotEmpty { return this }
        getSymbolsFromImportingScope(importScopeContext, fqName, KtScopeKind.DefaultSimpleImportingScope::class).ifNotEmpty { return this }
        getSymbolsFromImportingScope(importScopeContext, fqName, KtScopeKind.ExplicitStarImportingScope::class).ifNotEmpty { return this }
        getSymbolsFromImportingScope(importScopeContext, fqName, KtScopeKind.DefaultStarImportingScope::class).ifNotEmpty { return this }
        getNonImportedSymbolsByFullyQualifiedName(fqName).ifNotEmpty { return this }
        return emptyList()
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromDeclaration(name: Name, owner: KtDeclaration): List<KtSymbol> = buildList {
        if (owner is KtNamedDeclaration) {
            if (owner.nameAsName == name) {
                add(owner.getSymbol())
            }
        }
        if (owner is KtTypeParameterListOwner) {
            for (typeParameter in owner.typeParameters) {
                if (typeParameter.nameAsName == name) {
                    add(typeParameter.getTypeParameterSymbol())
                }
            }
        }
        if (owner is KtCallableDeclaration) {
            for (typeParameter in owner.valueParameters) {
                if (typeParameter.nameAsName == name) {
                    add(typeParameter.getParameterSymbol())
                }
            }
        }

        if (owner is KtClassOrObject) {
            owner.primaryConstructor?.let { addAll(getSymbolsFromDeclaration(name, it)) }
        }
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromParentMemberScopes(fqName: FqName, contextElement: KtElement): Collection<KtSymbol> {
        for (ktDeclaration in contextElement.parentsOfType<KtDeclaration>(withSelf = true)) {
            if (fqName.pathSegments().size == 1) {
                getSymbolsFromDeclaration(fqName.shortName(), ktDeclaration).ifNotEmpty { return this }
            }
            if (ktDeclaration is KtClassOrObject) {
                val symbol = ktDeclaration.getClassOrObjectSymbol() ?: continue

                val scope = listOfNotNull(
                    symbol.getMemberScope(),
                    getCompanionObjectMemberScope(symbol),
                ).asCompositeScope()

                val symbolsFromScope = getSymbolsFromMemberScope(fqName, scope)
                if (symbolsFromScope.isNotEmpty()) return symbolsFromScope
            }
        }
        return emptyList()
    }

    context(KtAnalysisSession)
    private fun getCompanionObjectMemberScope(classSymbol: KtClassOrObjectSymbol): KtScope? {
        val namedClassSymbol = classSymbol as? KtNamedClassOrObjectSymbol ?: return null
        val companionSymbol = namedClassSymbol.companionObject ?: return null
        return companionSymbol.getMemberScope()
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromPackageScope(fqName: FqName, contextElement: KtElement): Collection<KtDeclarationSymbol> {
        val packageFqName = contextElement.containingKtFile.packageFqName
        val packageSymbol = getPackageSymbolIfPackageExists(packageFqName) ?: return emptyList()
        val packageScope = packageSymbol.getPackageScope()
        return getSymbolsFromMemberScope(fqName, packageScope)
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromImportingScope(
        scopeContext: KtScopeContext,
        fqName: FqName,
        acceptScopeKind: KClass<out KtScopeKind>,
    ): Collection<KtDeclarationSymbol> {
        val importingScope = scopeContext.getCompositeScope { acceptScopeKind.java.isAssignableFrom(it::class.java) }
        return getSymbolsFromMemberScope(fqName, importingScope)
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromMemberScope(fqName: FqName, scope: KtScope): Collection<KtDeclarationSymbol> {
        val finalScope = fqName.pathSegments()
            .dropLast(1)
            .fold(scope) { currentScope, fqNamePart ->
                currentScope
                    .getClassifierSymbols(fqNamePart)
                    .mapNotNull { (it as? KtSymbolWithMembers)?.getDeclaredMemberScope() }
                    .toList()
                    .asCompositeScope()
            }

        return finalScope.getAllSymbolsFromScopeByShortName(fqName)
    }

    private fun KtScope.getAllSymbolsFromScopeByShortName(fqName: FqName): Collection<KtDeclarationSymbol> {
        val shortName = fqName.shortName()
        return buildSet {
            addAll(getCallableSymbols(shortName))
            addAll(getClassifierSymbols(shortName))
        }
    }

    context(KtAnalysisSession)
    private fun getNonImportedSymbolsByFullyQualifiedName(fqName: FqName): List<KtSymbol> = buildList {
        generateNameInterpretations(fqName).forEach { interpretation ->
            collectSymbolsByFqNameInterpretation(interpretation)
        }
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectSymbolsByFqNameInterpretation(
        interpretation: FqNameInterpretation,
    ) {
        when (interpretation) {
            is FqNameInterpretation.FqNameInterpretationAsCallableId -> {
                collectSymbolsByFqNameInterpretationAsCallableId(interpretation.callableId)
            }

            is FqNameInterpretation.FqNameInterpretationAsClassId -> {
                collectSymbolsByClassId(interpretation.classId)
            }

            is FqNameInterpretation.FqNameInterpretationAsPackage -> {
                collectSymbolsByPackage(interpretation.packageFqName)
            }
        }
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectSymbolsByPackage(packageFqName: FqName) {
        getPackageSymbolIfPackageExists(packageFqName)?.let(::add)
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectSymbolsByClassId(classId: ClassId) {
        getClassOrObjectSymbolByClassId(classId)?.let(::add)
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectSymbolsByFqNameInterpretationAsCallableId(callableId: CallableId) {
        when (val classId = callableId.classId) {
            null -> {
                addAll(getTopLevelCallableSymbols(callableId.packageName, callableId.callableName))
            }

            else -> {
                getClassOrObjectSymbolByClassId(classId)
                    ?.getDeclaredMemberScope()
                    ?.getCallableSymbols(callableId.callableName)
                    ?.let(::addAll)
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
                callable == null -> FqNameInterpretationAsClassId(ClassId(packageName, relativeClassName, false))
                else -> FqNameInterpretationAsCallableId(CallableId(packageName, relativeClassName.takeUnless { it.isRoot }, callable))
            }
        }
    }
}