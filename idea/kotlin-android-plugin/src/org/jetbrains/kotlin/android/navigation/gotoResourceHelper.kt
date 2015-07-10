/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.navigation

import org.jetbrains.android.util.AndroidResourceUtil
import com.intellij.psi.PsiElement
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiClass
import org.jetbrains.android.util.AndroidUtils
import com.android.SdkConstants
import org.jetbrains.android.augment.AndroidPsiElementFinder
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.JetExpression

private fun getReferenceExpression(element: PsiElement?): JetSimpleNameExpression? {
    return PsiTreeUtil.getParentOfType<JetSimpleNameExpression>(element, javaClass<JetSimpleNameExpression>())
}

// given 'R.a.b' returns info for all three parts of the expression 'a', 'b', 'R'
private fun getInfo(
        referenceExpression: JetSimpleNameExpression,
        facet: AndroidFacet
): AndroidResourceUtil.MyReferredResourceFieldInfo? {
    val info = getReferredInfo(referenceExpression, facet)
    if (info != null) return info

    val topMostQualified = referenceExpression.getParentQualified().getParentQualified() ?: return null
    val selectorCandidate = topMostQualified.getSelectorExpression() as? JetSimpleNameExpression ?: return null
    return getReferredInfo(selectorCandidate, facet)
}

private fun JetExpression?.getParentQualified(): JetDotQualifiedExpression? {
    return this?.getParent() as? JetDotQualifiedExpression
}

// returns info if passed expression is 'b' in 'R.a.b'
private fun getReferredInfo(
        lastPart: JetSimpleNameExpression,
        facet: AndroidFacet
): AndroidResourceUtil.MyReferredResourceFieldInfo? {
    val resFieldName = lastPart.getReferencedName()
    if (resFieldName.isEmpty()) return null

    val middlePart = getReceiverAsSimpleNameExpression(lastPart) ?: return null

    val resClassName = middlePart.getReferencedName()
    if (resClassName.isEmpty()) return null

    val firstPart = getReceiverAsSimpleNameExpression(middlePart) ?: return null

    val resolvedClass = firstPart.mainReference.resolve() as? PsiClass ?: return null

    //the following code is copied from
    // org.jetbrains.android.util.AndroidResourceUtil.getReferredResourceOrManifestField
    // (org.jetbrains.android.facet.AndroidFacet, com.intellij.psi.PsiReferenceExpression, java.lang.String, boolean)
    val classShortName = resolvedClass.getName()

    val fromManifest = AndroidUtils.MANIFEST_CLASS_NAME == classShortName

    if (!fromManifest && AndroidUtils.R_CLASS_NAME != classShortName) {
        return null
    }
    val qName = resolvedClass.getQualifiedName()

    if (SdkConstants.CLASS_R == qName || AndroidPsiElementFinder.INTERNAL_R_CLASS_QNAME == qName) {
        return AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, true, false)
    }
    val containingFile = resolvedClass.getContainingFile() ?: return null

    val isFromCorrectFile =
            if (fromManifest) AndroidResourceUtil.isManifestJavaFile(facet, containingFile)
            else AndroidResourceUtil.isRJavaFile(facet, containingFile)

    if (!isFromCorrectFile) {
        return null
    }

    return AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, false, fromManifest)
}

private fun getReceiverAsSimpleNameExpression(exp: JetSimpleNameExpression): JetSimpleNameExpression? {
    val receiver = exp.getReceiverExpression()
    return when (receiver) {
        is JetSimpleNameExpression -> {
            receiver
        }
        is JetDotQualifiedExpression -> {
            receiver.getSelectorExpression() as? JetSimpleNameExpression
        }
        else -> null
    }

}
