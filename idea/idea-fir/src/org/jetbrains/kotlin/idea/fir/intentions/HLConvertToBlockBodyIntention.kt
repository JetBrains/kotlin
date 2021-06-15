/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.intentions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.api.applicator.applicator
import org.jetbrains.kotlin.idea.fir.api.AbstractHLIntention
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicabilityRange
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInputProvider
import org.jetbrains.kotlin.idea.fir.api.applicator.inputProvider
import org.jetbrains.kotlin.idea.fir.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.idea.formatter.adjustLineIndent
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassErrorType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.util.resultingWhens
import org.jetbrains.kotlin.idea.util.setType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class HLConvertToBlockBodyIntention :
    AbstractHLIntention<KtDeclarationWithBody, HLConvertToBlockBodyIntention.Input>(KtDeclarationWithBody::class, applicator) {

    class Input(
        val returnType: KtType,
        val returnTypeIsUnit: Boolean,
        val returnTypeIsNothing: Boolean,
        val returnTypeString: String,
        val returnTypeClassId: ClassId?,
        val bodyTypeIsUnit: Boolean,
        val bodyTypeIsNothing: Boolean,
        val reformat: Boolean,
    ) : HLApplicatorInput

    override fun allowCaretInsideElement(element: PsiElement) = element !is KtDeclaration && super.allowCaretInsideElement(element)

    override val applicabilityRange: HLApplicabilityRange<KtDeclarationWithBody> get() = ApplicabilityRanges.SELF

    override val inputProvider: HLApplicatorInputProvider<KtDeclarationWithBody, Input>
        get() = inputProvider { psi ->
            if (psi is KtNamedFunction) {
                val returnType = psi.getReturnKtType()
                if (!psi.hasDeclaredReturnType() && returnType is KtClassErrorType) return@inputProvider null
            }
            createInputForDeclaration(psi, true)
        }

    companion object {
        fun KtAnalysisSession.createInputForDeclaration(declaration: KtDeclarationWithBody, reformat: Boolean): Input? {
            val body = declaration.bodyExpression ?: return null
            val returnType = declaration.getReturnKtType().approximateToSuperPublicDenotableOrSelf()
            val bodyType = body.getKtType()
            return Input(
                returnType,
                returnType.isUnit,
                returnType.isNothing,
                returnType.render(),
                returnType.expandedClassSymbol?.classIdIfNonLocal,
                bodyType.isUnit,
                bodyType.isNothing,
                reformat,
            )
        }

        val applicator = applicator<KtDeclarationWithBody, Input> {
            familyAndActionName(KotlinBundle.lazyMessage(("convert.to.block.body")))
            isApplicableByPsi { (it is KtNamedFunction || it is KtPropertyAccessor) && !it.hasBlockBody() && it.hasBody() }
            applyTo { declaration, input ->
                val body = declaration.bodyExpression!!

                val newBody = when (declaration) {
                    is KtNamedFunction -> {
                        if (!declaration.hasDeclaredReturnType() && !input.returnTypeIsUnit) {
                            declaration.setType(input.returnTypeString, input.returnTypeClassId)
                        }
                        generateBody(body, input, !input.returnTypeIsUnit && !input.returnTypeIsNothing)
                    }

                    is KtPropertyAccessor -> {
                        val parent = declaration.parent
                        if (parent is KtProperty && parent.typeReference == null) {
                            parent.setType(input.returnTypeString, input.returnTypeClassId)
                        }

                        generateBody(body, input, declaration.isGetter)
                    }

                    else -> throw RuntimeException("Unknown declaration type: $declaration")
                }

                declaration.equalsToken!!.delete()
                val replaced = body.replace(newBody)
                if (input.reformat) declaration.containingKtFile.adjustLineIndent(replaced.startOffset, replaced.endOffset)
            }
        }

        private fun generateBody(body: KtExpression, input: Input, returnsValue: Boolean): KtExpression {
            val factory = KtPsiFactory(body)
            if (input.bodyTypeIsUnit && body is KtNameReferenceExpression) return factory.createEmptyBody()
            val unitWhenAsResult = input.bodyTypeIsUnit && body.resultingWhens().isNotEmpty()
            val needReturn = returnsValue && (!input.bodyTypeIsUnit && !input.bodyTypeIsNothing)
            return if (needReturn || unitWhenAsResult) {
                val annotatedExpr = body as? KtAnnotatedExpression
                val returnedExpr = annotatedExpr?.baseExpression ?: body
                val block = factory.createSingleStatementBlock(factory.createExpressionByPattern("return $0", returnedExpr))
                val statement = block.firstStatement
                annotatedExpr?.annotationEntries?.forEach {
                    block.addBefore(it, statement)
                    block.addBefore(factory.createNewLine(), statement)
                }
                block
            } else {
                factory.createSingleStatementBlock(body)
            }
        }
    }
}