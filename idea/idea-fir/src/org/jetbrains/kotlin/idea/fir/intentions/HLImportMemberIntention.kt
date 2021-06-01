/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.api.applicator.applicator
import org.jetbrains.kotlin.idea.fir.api.AbstractHLIntention
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicabilityRange
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInputProvider
import org.jetbrains.kotlin.idea.fir.api.applicator.inputProvider
import org.jetbrains.kotlin.idea.fir.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenOption
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective

class HLImportMemberIntention : AbstractHLIntention<KtNameReferenceExpression, HLImportMemberIntention.Input>(
    KtNameReferenceExpression::class, Companion.applicator
), HighPriorityAction {
    override val applicabilityRange: HLApplicabilityRange<KtNameReferenceExpression> get() = ApplicabilityRanges.SELF

    override val inputProvider: HLApplicatorInputProvider<KtNameReferenceExpression, Input> = inputProvider { psi ->
        val symbol = psi.mainReference.resolveToSymbol() ?: return@inputProvider null
        computeInput(psi, symbol)
    }

    private fun KtAnalysisSession.computeInput(psi: KtNameReferenceExpression, symbol: KtSymbol): Input? {
        return when (symbol) {
            is KtConstructorSymbol,
            is KtClassOrObjectSymbol -> {
                val classId = if (symbol is KtClassOrObjectSymbol) {
                    symbol.classIdIfNonLocal
                } else {
                    (symbol as KtConstructorSymbol).containingClassIdIfNonLocal
                } ?: return null
                val shortenCommand = collectPossibleReferenceShortenings(
                    psi.containingKtFile,
                    classShortenOption = {
                        if (it.classIdIfNonLocal == classId)
                            ShortenOption.SHORTEN_AND_IMPORT
                        else
                            ShortenOption.DO_NOT_SHORTEN
                    }, callableShortenOption = {
                        if (it is KtConstructorSymbol && it.containingClassIdIfNonLocal == classId)
                            ShortenOption.SHORTEN_AND_IMPORT
                        else
                            ShortenOption.DO_NOT_SHORTEN
                    })
                if (shortenCommand.isEmpty) return null
                Input(classId.asSingleFqName(), shortenCommand)
            }
            is KtCallableSymbol -> {
                val callableId = symbol.callableIdIfNonLocal ?: return null
                if (callableId.callableName.isSpecial) return null
                if (!canBeImported(symbol)) return null
                val shortenCommand = collectPossibleReferenceShortenings(
                    psi.containingKtFile,
                    classShortenOption = { ShortenOption.DO_NOT_SHORTEN },
                    callableShortenOption = {
                        if (it.callableIdIfNonLocal == callableId)
                            ShortenOption.SHORTEN_AND_IMPORT
                        else
                            ShortenOption.DO_NOT_SHORTEN
                    }
                )
                if (shortenCommand.isEmpty) return null
                Input(callableId.asSingleFqName(), shortenCommand)
            }
            else -> return null
        }
    }

    class Input(val fqName: FqName, val shortenCommand: ShortenCommand) : HLApplicatorInput

    companion object {
        val applicator = applicator<KtNameReferenceExpression, Input> {
            familyName(KotlinBundle.lazyMessage("add.import.for.member"))
            actionName { _, input -> KotlinBundle.message("add.import.for.0", input.fqName.asString()) }
            isApplicableByPsi {
                // Ignore simple name expressions or already imported names.
                if (it.getQualifiedElement() == it || it.isInImportDirective()) return@isApplicableByPsi false
                true
            }
            applyTo { _, input ->
                input.shortenCommand.invokeShortening()
            }
        }

        private fun KtAnalysisSession.canBeImported(symbol: KtCallableSymbol): Boolean {
            if (symbol is KtEnumEntrySymbol) return true
            if (symbol.origin == KtSymbolOrigin.JAVA) {
                return when (symbol) {
                    is KtFunctionSymbol -> symbol.isStatic
                    is KtPropertySymbol -> symbol.isStatic
                    is KtJavaFieldSymbol -> symbol.isStatic
                    else -> false
                }
            } else {
                if ((symbol as? KtSymbolWithKind)?.symbolKind == KtSymbolKind.TOP_LEVEL) return true
                val containingClass = symbol.getContainingSymbol() as? KtClassOrObjectSymbol ?: return true
                return containingClass.classKind == KtClassKind.OBJECT || containingClass.classKind == KtClassKind.COMPANION_OBJECT
            }
        }
    }
}