/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration

internal object KDocReferenceResolver {

    context(KtAnalysisSession)
    internal fun resolveKdocFqName(fqName: FqName, owner: KtDeclaration?): Collection<KtSymbol> = buildSet {
        collectSymbolsByFullyQualifiedName(fqName)

        if (owner != null) {
            collectSymbolsFromPositionScopes(owner, fqName)
            val ownerSymbol = owner.getSymbol()

            val pathSegments = fqName.pathSegments()
            if (pathSegments.size == 1) {
                val singleSegment = pathSegments.single()
                when (ownerSymbol) {
                    is KtConstructorSymbol -> {
                        collectConstructorParameterSymbols(ownerSymbol, singleSegment)
                    }

                    is KtNamedClassOrObjectSymbol -> {
                        collectPrimaryConstructorParameterSymbols(ownerSymbol, singleSegment)
                    }

                    else -> {}
                }
            }
        }
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectPrimaryConstructorParameterSymbols(owner: KtNamedClassOrObjectSymbol, name: Name) {
        val primaryConstructor = owner.getDeclaredMemberScope().getConstructors().firstOrNull { it.isPrimary } ?: return
        collectConstructorParameterSymbols(primaryConstructor, name)
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectSymbolsFromPositionScopes(owner: KtDeclaration, fqName: FqName) {
        val initialScope = owner.containingKtFile.getScopeContextForPosition(owner).getCompositeScope()

        val scope = fqName.pathSegments()
            .dropLast(1)
            .fold(initialScope) { currentScope, fqNamePart ->
                currentScope
                    .getClassifierSymbols { it == fqNamePart }
                    .mapNotNull { (it as? KtSymbolWithMembers)?.getDeclaredMemberScope() }
                    .toList()
                    .asCompositeScope()
            }

        val shortName = fqName.shortName()
        addAll(scope.getCallableSymbols { it == shortName })
        addAll(scope.getClassifierSymbols { it == shortName })
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectConstructorParameterSymbols(owner: KtFunctionLikeSymbol, name: Name) {
        for (valueParameter in owner.valueParameters) {
            if (valueParameter.name == name) {
                add(valueParameter)
            }
        }
    }

    context(KtAnalysisSession)
    private fun MutableCollection<KtSymbol>.collectSymbolsByFullyQualifiedName(fqName: FqName) {
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
                    ?.getCallableSymbols { it == callableId.callableName }
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