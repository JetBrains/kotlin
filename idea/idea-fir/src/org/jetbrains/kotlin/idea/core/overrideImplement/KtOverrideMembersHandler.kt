/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.overrideImplement

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.core.util.KotlinIdeaCoreBundle
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.KtIconProvider.getIcon
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class KtOverrideMembersHandler : KtGenerateMembersHandler() {
    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    override fun collectMembersToGenerate(classOrObject: KtClassOrObject): Collection<KtClassMember> {
        return hackyAllowRunningOnEdt {
            analyse(classOrObject) {
                collectMembers(classOrObject)
            }
        }
    }

    fun KtAnalysisSession.collectMembers(classOrObject: KtClassOrObject): List<KtClassMember> {
        val classOrObjectSymbol = classOrObject.getClassOrObjectSymbol()
        return getOverridableMembers(classOrObjectSymbol).map { (symbol, bodyType, containingSymbol) ->
            KtClassMember(
                KtClassMemberInfo(
                    symbol,
                    symbol.render(renderOption),
                    getIcon(symbol),
                    containingSymbol?.classIdIfNonLocal?.asSingleFqName()?.toString() ?: containingSymbol?.name?.asString(),
                    containingSymbol?.let { getIcon(it) }
                ),
                bodyType,
                preferConstructorParameter = false
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun KtAnalysisSession.getOverridableMembers(classOrObjectSymbol: KtClassOrObjectSymbol): List<OverrideMember> {
        return buildList {
            classOrObjectSymbol.getMemberScope().getCallableSymbols().forEach { symbol ->
                if (!symbol.isVisibleInClass(classOrObjectSymbol)) return@forEach
                val implementationStatus = symbol.getImplementationStatus(classOrObjectSymbol) ?: return@forEach
                if (!implementationStatus.isOverridable) return@forEach

                val intersectionSymbols = symbol.getIntersectionOverriddenSymbols()
                val symbolsToProcess = if (intersectionSymbols.size <= 1) {
                    listOf(symbol)
                } else {
                    val nonAbstractMembers = intersectionSymbols.filter { (it as? KtSymbolWithModality)?.modality != Modality.ABSTRACT }
                    // If there are non-abstract members, we only want to show override for these non-abstract members. Otherwise, show any
                    // abstract member to override.
                    nonAbstractMembers.ifEmpty {
                        listOf(intersectionSymbols.first())
                    }
                }

                val hasNoSuperTypesExceptAny = classOrObjectSymbol.superTypes.singleOrNull()?.type?.isAny == true
                for (symbolToProcess in symbolsToProcess) {
                    val originalOverriddenSymbol = symbolToProcess.originalOverriddenSymbol
                    val containingSymbol = originalOverriddenSymbol?.originalContainingClassForOverride

                    val bodyType = when {
                        classOrObjectSymbol.classKind == KtClassKind.INTERFACE && containingSymbol?.classIdIfNonLocal == StandardClassIds.Any -> {
                            if (hasNoSuperTypesExceptAny) {
                                // If an interface does not extends any other interfaces, FE1.0 simply skips members of `Any`. So we mimic
                                // the same behavior. See idea/testData/codeInsight/overrideImplement/noAnyMembersInInterface.kt
                                continue
                            } else {
                                BodyType.NO_BODY
                            }
                        }
                        (originalOverriddenSymbol as? KtSymbolWithModality)?.modality == Modality.ABSTRACT ->
                            BodyType.FROM_TEMPLATE
                        symbolsToProcess.size > 1 ->
                            BodyType.QUALIFIED_SUPER
                        else ->
                            BodyType.SUPER
                    }
                    // Ideally, we should simply create `KtClassMember` here and remove the intermediate `OverrideMember` data class. But
                    // that doesn't work because this callback function is holding a read lock and `symbol.render(renderOption)` requires
                    // the write lock.
                    // Hence, we store the data in an intermediate `OverrideMember` data class and do the rendering later in the `map` call.
                    add(OverrideMember(symbolToProcess, bodyType, containingSymbol))
                }
            }
        }
    }

    private data class OverrideMember(val symbol: KtCallableSymbol, val bodyType: BodyType, val containingSymbol: KtClassOrObjectSymbol?)

    override fun getChooserTitle() = KotlinIdeaCoreBundle.message("override.members.handler.title")

    override fun getNoMembersFoundHint() = KotlinIdeaCoreBundle.message("override.members.handler.no.members.hint")
}