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

package org.jetbrains.kotlin.android

import com.android.SdkConstants
import com.android.resources.ResourceType.*
import com.android.tools.idea.AndroidPsiUtils.ResourceReferenceType.FRAMEWORK
import com.android.tools.idea.rendering.GutterIconRenderer
import com.android.tools.idea.rendering.ResourceHelper
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.android.AndroidColorAnnotator
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.ResourceReferenceAnnotatorUtil.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class AndroidResourceReferenceAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val reference = element as? KtReferenceExpression ?: return
        val androidFacet = AndroidFacet.getInstance(element) ?: return
        val referenceTarget = reference.getResourceReferenceTargetDescriptor() ?: return
        val resourceType = referenceTarget.getAndroidResourceType() ?: return

        if (resourceType != COLOR && resourceType != DRAWABLE && resourceType != MIPMAP) {
            return
        }

        val referenceType = referenceTarget.getResourceReferenceType()
        val configuration = pickConfiguration(androidFacet, androidFacet.module, element.containingFile) ?: return
        val resourceValue = findResourceValue(resourceType,
                                              reference.text,
                                              referenceType == FRAMEWORK,
                                              androidFacet.module,
                                              configuration) ?: return

        val resourceResolver = configuration.resourceResolver ?: return

        if (resourceType == COLOR) {
            val color = ResourceHelper.resolveColor(resourceResolver, resourceValue, element.project)
            if (color != null) {
                val annotation = holder.createInfoAnnotation(element, null)
                annotation.gutterIconRenderer = ColorRenderer(element, color)
            }
        }
        else {
            var file = ResourceHelper.resolveDrawable(resourceResolver, resourceValue, element.project)
            if (file != null && file.path.endsWith(SdkConstants.DOT_XML)) {
                file = pickBitmapFromXml(file, resourceResolver, element.project)
            }
            val iconFile = AndroidColorAnnotator.pickBestBitmap(file)
            if (iconFile != null) {
                val annotation = holder.createInfoAnnotation(element, null)
                annotation.gutterIconRenderer = GutterIconRenderer(element, iconFile)
            }
        }
    }

    private fun KtReferenceExpression.getResourceReferenceTargetDescriptor(): JavaPropertyDescriptor? =
            analyze(BodyResolveMode.PARTIAL)[BindingContext.REFERENCE_TARGET, this] as? JavaPropertyDescriptor
}
