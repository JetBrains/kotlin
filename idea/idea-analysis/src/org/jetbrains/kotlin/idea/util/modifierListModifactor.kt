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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun KtModifierListOwner.addAnnotation(
        annotationFqName: FqName,
        annotationInnerText: String? = null,
        whiteSpaceText: String = "\n",
        addToExistingAnnotation: ((KtAnnotationEntry) -> Boolean)? = null): Boolean {
    val annotationText = when (annotationInnerText) {
        null -> "@${annotationFqName.asString()}"
        else -> "@${annotationFqName.asString()}($annotationInnerText)"
    }

    val psiFactory = KtPsiFactory(this)
    val modifierList = modifierList

    if (modifierList == null) {
        // create a modifier list from scratch
        val newModifierList = psiFactory.createModifierList(annotationText)
        val replaced = KtPsiUtil.replaceModifierList(this, newModifierList)!!
        val whiteSpace = psiFactory.createWhiteSpace(whiteSpaceText)
        addAfter(whiteSpace, replaced)

        ShortenReferences.DEFAULT.process(replaced)

        return true
    }

    val entry = findAnnotation(annotationFqName)
    if (entry == null) {
        // no annotation
        val newAnnotation = psiFactory.createAnnotationEntry(annotationText)
        val addedAnnotation = modifierList.addBefore(newAnnotation, modifierList.firstChild) as KtElement
        val whiteSpace = psiFactory.createWhiteSpace(whiteSpaceText)
        modifierList.addAfter(whiteSpace, addedAnnotation)

        ShortenReferences.DEFAULT.process(addedAnnotation)

        return true
    }

    if (addToExistingAnnotation != null) {
        return addToExistingAnnotation(entry)
    }

    return false
}

fun KtAnnotated.findAnnotation(annotationFqName: FqName): KtAnnotationEntry? {
    if (annotationEntries.isEmpty()) return null

    val context = analyze(bodyResolveMode = BodyResolveMode.PARTIAL)
    val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return null

    // Make sure all annotations are resolved
    descriptor.annotations.toList()

    return annotationEntries.firstOrNull { entry -> context.get(BindingContext.ANNOTATION, entry)?.fqName == annotationFqName }
}
