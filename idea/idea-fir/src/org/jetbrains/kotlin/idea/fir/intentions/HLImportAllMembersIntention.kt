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
import org.jetbrains.kotlin.idea.frontend.api.calls.getSuccessCallSymbolOrNull
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenOption
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective

class HLImportAllMembersIntention :
    AbstractHLIntention<KtExpression, HLImportAllMembersIntention.Input>(KtExpression::class, Companion.applicator), HighPriorityAction {
    override val applicabilityRange: HLApplicabilityRange<KtExpression> get() = ApplicabilityRanges.SELF

    override val inputProvider: HLApplicatorInputProvider<KtExpression, Input> = inputProvider { psi ->
        val target = psi.actualReference?.resolveToSymbol() as? KtNamedClassOrObjectSymbol ?: return@inputProvider null
        val classId = target.classIdIfNonLocal ?: return@inputProvider null
        if (target.origin != KtSymbolOrigin.JAVA &&
            (target.classKind == KtClassKind.OBJECT ||
                    // One cannot use on-demand import for properties or functions declared inside objects
                    isReferenceToObjectMemberOrUnresolved(psi))
        ) {
            // Import all members of an object is not supported by Kotlin.
            return@inputProvider null
        }
        val shortenCommand = collectPossibleReferenceShortenings(
            psi.containingKtFile,
            classShortenOption = {
                if (it.classIdIfNonLocal?.isNestedClassIn(classId) == true) {
                    ShortenOption.SHORTEN_AND_STAR_IMPORT
                } else {
                    ShortenOption.DO_NOT_SHORTEN
                }
            },
            callableShortenOption = {
                val containingClassId = if (it is KtConstructorSymbol) {
                    it.containingClassIdIfNonLocal?.outerClassId
                } else {
                    it.callableIdIfNonLocal?.classId
                }
                if (containingClassId == classId) {
                    ShortenOption.SHORTEN_AND_STAR_IMPORT
                } else {
                    ShortenOption.DO_NOT_SHORTEN
                }
            }
        )
        if (shortenCommand.isEmpty) return@inputProvider null
        Input(classId.asSingleFqName(), shortenCommand)
    }

    private fun ClassId.isNestedClassIn(classId: ClassId) =
        packageFqName == classId.packageFqName && relativeClassName.parent() == classId.relativeClassName

    class Input(val fqName: FqName, val shortenCommand: ShortenCommand) : HLApplicatorInput

    companion object {
        val applicator = applicator<KtExpression, Input> {
            familyName(KotlinBundle.lazyMessage("import.members.with"))
            actionName { _, input -> KotlinBundle.message("import.members.from.0", input.fqName.asString()) }
            isApplicableByPsi { it.isOnTheLeftOfQualificationDot && !it.isInImportDirective() }
            applyTo { _, input ->
                input.shortenCommand.invokeShortening()
            }
        }

        private val KtExpression.isOnTheLeftOfQualificationDot: Boolean
            get() {
                return when (val parent = parent) {
                    is KtDotQualifiedExpression -> this == parent.receiverExpression
                    is KtUserType -> {
                        val grandParent = parent.parent as? KtUserType ?: return false
                        grandParent.qualifier == parent && parent.referenceExpression == this
                    }
                    else -> false
                }
            }

        private val KtExpression.actualReference: KtReference?
            get() = when (this) {
                is KtDotQualifiedExpression -> selectorExpression?.mainReference ?: mainReference
                else -> mainReference
            }

        private fun KtAnalysisSession.isReferenceToObjectMemberOrUnresolved(qualifiedAccess: KtExpression): Boolean {
            val selectorExpression: KtExpression? = qualifiedAccess.getQualifiedExpressionForReceiver()?.selectorExpression
            val referencedSymbol = when (selectorExpression) {
                is KtCallExpression -> selectorExpression.resolveCall()?.targetFunction?.getSuccessCallSymbolOrNull()
                is KtNameReferenceExpression -> selectorExpression.mainReference.resolveToSymbol()
                else -> return false
            } as? KtSymbolWithKind ?: return true
            if (referencedSymbol is KtConstructorSymbol) return false
            return (referencedSymbol.getContainingSymbol() as? KtClassOrObjectSymbol)?.classKind?.isObject ?: true
        }
    }
}