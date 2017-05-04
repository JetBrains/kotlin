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

package org.jetbrains.kotlin.android.quickfix

import com.android.SdkConstants
import com.android.tools.lint.checks.ApiDetector.REQUIRES_API_ANNOTATION
import com.intellij.codeInsight.FileModificationService
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.android.inspections.lint.AndroidLintQuickFix
import org.jetbrains.android.inspections.lint.AndroidQuickfixContexts
import org.jetbrains.android.util.AndroidBundle
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*


class AddTargetApiQuickFix(
        val api: Int,
        val useRequiresApi: Boolean
) : AndroidLintQuickFix {

    private companion object {
        val FQNAME_TARGET_API = FqName(SdkConstants.FQCN_TARGET_API)
        val FQNAME_REQUIRES_API = FqName(REQUIRES_API_ANNOTATION)
    }

    override fun isApplicable(startElement: PsiElement, endElement: PsiElement, contextType: AndroidQuickfixContexts.ContextType): Boolean =
            getAnnotationContainer(startElement, useRequiresApi) != null

    override fun getName(): String = getAnnotationValue(false).let {
        if (useRequiresApi) {
            // Not Available in Android plugin 2.0
            // AndroidBundle.message("android.lint.fix.add.requires.api", it)
            "Add @RequiresApi($it) Annotation"
        } else {
            AndroidBundle.message("android.lint.fix.add.target.api", it)
        }
    }

    override fun apply(startElement: PsiElement, endElement: PsiElement, context: AndroidQuickfixContexts.Context) {
        val annotationContainer = getAnnotationContainer(startElement, useRequiresApi) ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(annotationContainer)) {
            return
        }

        if (annotationContainer is KtModifierListOwner) {
             annotationContainer.addAnnotation(
                     if (useRequiresApi) FQNAME_REQUIRES_API else FQNAME_TARGET_API,
                     getAnnotationValue(true),
                     whiteSpaceText = "\n")
        }
    }

    private fun getAnnotationValue(fullyQualified: Boolean) = getVersionField(api, fullyQualified)

    private fun getAnnotationContainer(element: PsiElement, useRequiresApi: Boolean) =
            PsiTreeUtil.findFirstParent(element) {
                if (useRequiresApi)
                    it.isRequiresApiAnnotationValidTarget()
                else
                    it.isTargetApiAnnotationValidTarget()
            }


    // TODO: KtFunctionLiteral is not supported now because addAnnotation fails to shorten references, investigate
    private fun PsiElement.isRequiresApiAnnotationValidTarget() = this is KtClassOrObject ||
                                                                  (this is KtFunction && this !is KtFunctionLiteral) ||
                                                                  (this is KtProperty && !this.isLocal)

    // TODO: KtFunctionLiteral is not supported now because addAnnotation fails to shorten references, investigate
    private fun PsiElement.isTargetApiAnnotationValidTarget() = this is KtClassOrObject ||
                                                                (this is KtFunction && this !is KtFunctionLiteral)
}