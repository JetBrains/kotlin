/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.api.applicator.applicator
import org.jetbrains.kotlin.idea.fir.api.fixes.HLQuickFix
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenOption
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassKind
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.util.generateWhenBranches
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtWhenExpression

object AddWhenRemainingBranchFixFactories {
    class Input(val whenMissingCases: List<WhenMissingCase>, val enumToStarImport: ClassId?) : HLApplicatorInput

    val applicator: HLApplicator<KtWhenExpression, Input> = getApplicator(false)
    val applicatorUsingStarImport: HLApplicator<KtWhenExpression, Input> = getApplicator(true)

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    private fun getApplicator(useStarImport: Boolean = false) = applicator<KtWhenExpression, Input> {
        familyAndActionName(
            if (useStarImport) KotlinBundle.lazyMessage("fix.add.remaining.branches.with.star.import")
            else KotlinBundle.lazyMessage("fix.add.remaining.branches")
        )
        applyTo { whenExpression, input ->
            if (useStarImport) assert(input.enumToStarImport != null)
            generateWhenBranches(whenExpression, input.whenMissingCases)
            val shortenCommand = hackyAllowRunningOnEdt {
                analyse(whenExpression) {
                    collectPossibleReferenceShorteningsInElement(
                        whenExpression,
                        callableShortenOption = {
                            if (useStarImport && it.callableIdIfNonLocal?.classId == input.enumToStarImport) {
                                ShortenOption.SHORTEN_AND_STAR_IMPORT
                            } else {
                                ShortenOption.DO_NOT_SHORTEN
                            }
                        })

                }
            }
            shortenCommand.invokeShortening()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    val noElseInWhen = diagnosticFixFactory(KtFirDiagnostic.NoElseInWhen::class) { diagnostic ->
        val whenExpression = diagnostic.psi
        val subjectExpression = whenExpression.subjectExpression ?: return@diagnosticFixFactory emptyList()

        buildList {
            val missingCases = diagnostic.missingWhenCases.takeIf {
                it.isNotEmpty() && it.singleOrNull() != WhenMissingCase.Unknown
            } ?: return@buildList

            add(HLQuickFix(whenExpression, Input(missingCases, null), applicator))
            val baseClassSymbol = subjectExpression.getKtType().expandedClassSymbol ?: return@buildList
            val enumToStarImport = baseClassSymbol.classIdIfNonLocal
            if (baseClassSymbol.classKind == KtClassKind.ENUM_CLASS && enumToStarImport != null) {
                add(HLQuickFix(whenExpression, Input(missingCases, enumToStarImport), applicatorUsingStarImport))
            }
        }
    }
}