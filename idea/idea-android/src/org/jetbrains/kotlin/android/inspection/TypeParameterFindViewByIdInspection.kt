/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.inspection

import com.android.tools.idea.model.AndroidModuleInfo
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isUnsafeCast
import org.jetbrains.kotlin.psi.psiUtil.addTypeArgument

class TypeParameterFindViewByIdInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        val compileSdk = AndroidFacet.getInstance(session.file)
            ?.let { facet -> AndroidModuleInfo.getInstance(facet) }
            ?.buildSdkVersion
            ?.apiLevel

        if (compileSdk == null || compileSdk < 26) {
            return KtVisitorVoid()
        }

        return object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                if (expression.calleeExpression?.text != "findViewById" || expression.typeArguments.isNotEmpty()) {
                    return
                }

                val parentCast = (expression.parent as? KtBinaryExpressionWithTypeRHS)?.takeIf { isUnsafeCast(it) } ?: return
                val typeText = parentCast.right?.getTypeTextWithoutQuestionMark() ?: return
                val callableDescriptor = expression.resolveToCall()?.resultingDescriptor ?: return
                if (callableDescriptor.name.asString() != "findViewById" || callableDescriptor.typeParameters.size != 1) {
                    return
                }

                holder.registerProblem(
                    parentCast,
                    "Can be converted to findViewById<$typeText>(...)",
                    ConvertCastToFindViewByIdWithTypeParameter()
                )
            }
        }
    }

    class ConvertCastToFindViewByIdWithTypeParameter : LocalQuickFix {
        override fun getFamilyName(): String = "Convert cast to findViewById with type parameter"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val cast = descriptor.psiElement as? KtBinaryExpressionWithTypeRHS ?: return
            val typeText = cast.right?.getTypeTextWithoutQuestionMark() ?: return
            val call = cast.left as? KtCallExpression ?: return

            val newCall = call.copy() as KtCallExpression
            val typeArgument = KtPsiFactory(call).createTypeArgument(typeText)
            newCall.addTypeArgument(typeArgument)

            cast.replace(newCall)
        }
    }

    companion object {
        fun KtTypeReference.getTypeTextWithoutQuestionMark(): String? =
            (typeElement as? KtNullableType)?.innerType?.text ?: typeElement?.text
    }
}