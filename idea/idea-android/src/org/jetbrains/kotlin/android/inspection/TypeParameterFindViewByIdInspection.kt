/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isUnsafeCast
import org.jetbrains.kotlin.psi.psiUtil.addTypeArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class TypeParameterFindViewByIdInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return KtVisitorVoid()
/*
        val compileSdk = AndroidFacet.getInstance(session.file)?.androidModuleInfo?.buildSdkVersion?.apiLevel
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
                val callableDescriptor = expression.getResolvedCall(expression.analyze())?.resultingDescriptor ?: return
                if (callableDescriptor.name.asString() != "findViewById" || callableDescriptor.typeParameters.size != 1) {
                    return
                }

                holder.registerProblem(
                        parentCast,
                        "Can be converted to findViewById<$typeText>(...)",
                        ConvertCastToFindViewByIdWithTypeParameter())
            }
        } */
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