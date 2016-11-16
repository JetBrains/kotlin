/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android

import com.android.SdkConstants
import com.android.SdkConstants.ANDROID_PKG
import com.android.SdkConstants.R_CLASS
import com.android.resources.ResourceType
import com.android.tools.idea.AndroidPsiUtils
import com.android.tools.idea.AndroidPsiUtils.ResourceReferenceType.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.android.augment.AndroidPsiElementFinder
import org.jetbrains.android.dom.AndroidAttributeValue
import org.jetbrains.android.dom.manifest.Manifest
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.android.util.AndroidResourceUtil.isManifestJavaFile
import org.jetbrains.android.util.AndroidResourceUtil.isRJavaFile
import org.jetbrains.android.util.AndroidUtils
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.PsiSourceFile

internal fun KtClass.findComponentDeclarationInManifest(manifest: Manifest): AndroidAttributeValue<PsiClass>? {
    val application = manifest.application ?: return null
    val type = (unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) as? ClassDescriptor)?.defaultType ?: return null

    return when {
        type.isSubclassOf(AndroidUtils.ACTIVITY_BASE_CLASS_NAME) ->
            application.activities?.find { it.activityClass.value?.qualifiedName == fqName?.asString() }?.activityClass
        type.isSubclassOf(AndroidUtils.SERVICE_CLASS_NAME) ->
            application.services?.find { it.serviceClass.value?.qualifiedName == fqName?.asString() }?.serviceClass
        type.isSubclassOf(AndroidUtils.RECEIVER_CLASS_NAME) ->
            application.receivers?.find { it.receiverClass.value?.qualifiedName == fqName?.asString() }?.receiverClass
        type.isSubclassOf(AndroidUtils.PROVIDER_CLASS_NAME) ->
            application.providers?.find { it.providerClass.value?.qualifiedName == fqName?.asString() }?.providerClass
        else -> null
    }
}

internal fun PsiElement.getAndroidFacetForFile(): AndroidFacet? {
    val file = containingFile ?: return null
    return AndroidFacet.getInstance(file)
}

internal fun JavaPropertyDescriptor.getAndroidResourceType(): ResourceType? {
    if (getResourceReferenceType() == NONE) {
        return null
    }

    val containingClass = containingDeclaration as? JavaClassDescriptor ?: return null
    return ResourceType.getEnum(containingClass.name.asString())
}

internal fun JavaPropertyDescriptor.getResourceReferenceType(): AndroidPsiUtils.ResourceReferenceType {
    val containingClass = containingDeclaration as? JavaClassDescriptor ?: return NONE
    val rClass = containingClass.containingDeclaration as? JavaClassDescriptor ?: return NONE

    if (R_CLASS == rClass.name.asString()) {
        return if ((rClass.containingDeclaration as? PackageFragmentDescriptor)?.fqName?.asString() == ANDROID_PKG) {
            FRAMEWORK
        }
        else {
            APP
        }
    }

    return NONE
}

internal fun getReferredResourceOrManifestField(facet: AndroidFacet, expression: KtSimpleNameExpression, localOnly: Boolean)
        = getReferredResourceOrManifestField(facet, expression, null, localOnly)

internal fun getReferredResourceOrManifestField(facet: AndroidFacet, expression: KtSimpleNameExpression,
                                       className: String?, localOnly: Boolean): AndroidResourceUtil.MyReferredResourceFieldInfo? {
    val resFieldName = expression.getReferencedName()
    val resClassReference = expression.getPreviousInQualifiedChain() as? KtSimpleNameExpression ?: return null
    val resClassName = resClassReference.getReferencedName()

    if (resClassName.isEmpty() || className != null && className != resClassName) {
        return null
    }

    val rClassReference = resClassReference.getPreviousInQualifiedChain() as? KtSimpleNameExpression ?: return null
    val rClassDescriptor = rClassReference.analyze(BodyResolveMode.PARTIAL)
                                   .get(BindingContext.REFERENCE_TARGET, rClassReference) as? ClassDescriptor ?: return null

    val rClassShortName = rClassDescriptor.name.asString()
    val fromManifest = AndroidUtils.MANIFEST_CLASS_NAME == rClassShortName

    if (!fromManifest && AndroidUtils.R_CLASS_NAME != rClassShortName) {
        return null
    }

    if (!localOnly) {
        val qName = rClassDescriptor.fqNameSafe.asString()

        if (SdkConstants.CLASS_R == qName || AndroidPsiElementFinder.INTERNAL_R_CLASS_QNAME == qName) {
            return AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, facet.module, true, false)
        }
    }

    val containingFile = (rClassDescriptor.source.containingFile as? PsiSourceFile)?.psiFile ?: return null
    if (if (fromManifest) !isManifestJavaFile(facet, containingFile) else !isRJavaFile(facet, containingFile)) {
        return null
    }

    return AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, facet.module, false, false)
}

private fun KtExpression.getPreviousInQualifiedChain(): KtExpression? {
    val receiverExpression = getQualifiedExpressionForSelector()?.receiverExpression
    return (receiverExpression as? KtQualifiedExpression)?.selectorExpression ?: receiverExpression
}