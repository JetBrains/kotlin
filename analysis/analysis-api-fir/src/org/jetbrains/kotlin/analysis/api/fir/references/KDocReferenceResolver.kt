/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal object KDocReferenceResolver {

    context(KtAnalysisSession)
    internal fun resolveKdocFqName(fqName: FqName, owner: KtDeclaration?): Collection<KtSymbol> {
        if (owner != null) {
            if (fqName.pathSegments().size == 1) {
                getSymbolsFromDeclaration(fqName.shortName(), owner).ifNotEmpty { return this }
            }
            getSymbolsFromParentMemberScopes(fqName, owner).ifNotEmpty { return this }
            getSymbolsFromFileScope(fqName, owner).ifNotEmpty { return this }
            getSymbolsFromImportingScope(fqName, owner).ifNotEmpty { return this }
        }
        return collectSymbolsByFullyQualifiedName(fqName)
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
    private fun getSymbolsFromParentMemberScopes(fqName: FqName, owner: KtDeclaration): Collection<KtSymbol> {
        for (parentClass in owner.parentsOfType<KtClassOrObject>(withSelf = true)) {
            val symbol = parentClass.getClassOrObjectSymbol() ?: continue
            val scope = listOf(symbol.getMemberScope(), symbol.getStaticMemberScope()).asCompositeScope()
            val symbolsFromScope = getSymbolsFromMemberScope(fqName, scope)
            if (symbolsFromScope.isNotEmpty()) return symbolsFromScope
        }
        return emptyList()
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromFileScope(fqName: FqName, owner: KtDeclaration): Collection<KtSymbol> {
        val fileScope = owner.containingKtFile.getFileSymbol().getFileScope()
        return getSymbolsFromMemberScope(fqName, fileScope)
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromImportingScope(fqName: FqName, owner: KtDeclaration): Collection<KtDeclarationSymbol> {
        val importingScope = owner.containingKtFile.getImportingScopeContext().getCompositeScope()
        return getSymbolsFromMemberScope(fqName, importingScope)
    }

    context(KtAnalysisSession)
    private fun getSymbolsFromMemberScope(fqName: FqName, scope: KtScope): Collection<KtDeclarationSymbol> = buildSet {
        val finalScope = fqName.pathSegments()
            .dropLast(1)
            .fold(scope) { currentScope, fqNamePart ->
                currentScope
                    .getClassifierSymbols(fqNamePart)
                    .mapNotNull { (it as? KtSymbolWithMembers)?.getDeclaredMemberScope() }
                    .toList()
                    .asCompositeScope()
            }

        val shortName = fqName.shortName()
        addAll(finalScope.getCallableSymbols(shortName))
        addAll(finalScope.getClassifierSymbols(shortName))
    }

    context(KtAnalysisSession)
    private fun collectSymbolsByFullyQualifiedName(fqName: FqName): List<KtSymbol> = buildList {
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