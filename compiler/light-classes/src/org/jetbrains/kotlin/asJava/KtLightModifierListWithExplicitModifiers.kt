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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KtLightModifierListWithExplicitModifiers(
        private val owner: KtLightElement<*, *>,
        modifiers: Array<String>
) : LightModifierList(owner.manager, KotlinLanguage.INSTANCE, *modifiers) {
    abstract val delegate: PsiAnnotationOwner

    private val _annotations by lazy { computeAnnotations(this, delegate) }

    override fun getParent() = owner

    override fun getAnnotations(): Array<out PsiAnnotation> = _annotations.value

    override fun getApplicableAnnotations() = delegate.applicableAnnotations

    override fun findAnnotation(@NonNls qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }

    override fun addAnnotation(@NonNls qualifiedName: String) = delegate.addAnnotation(qualifiedName)
}

class KtLightModifierList(
        private val delegate: PsiModifierList,
        private val owner: PsiModifierListOwner
): PsiModifierList by delegate {
    private val _annotations by lazy { computeAnnotations(this, delegate) }

    override fun getAnnotations(): Array<out PsiAnnotation> = _annotations.value

    override fun findAnnotation(@NonNls qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }

    override fun addAnnotation(@NonNls qualifiedName: String) = delegate.addAnnotation(qualifiedName)

    override fun getParent() = owner
}

internal fun computeAnnotations(lightElement: PsiModifierList,
                                delegate: PsiAnnotationOwner): CachedValue<Array<out PsiAnnotation>> {
    val cacheManager = CachedValuesManager.getManager(lightElement.project)
    return cacheManager.createCachedValue<Array<out PsiAnnotation>>(
            {
                val declaration = (lightElement.parent as? KtLightElement<*, *>)?.getOrigin() as? KtDeclaration
                val descriptor = declaration?.let { LightClassGenerationSupport.getInstance(lightElement.project).resolveToDescriptor(it) }
                val ktAnnotations = descriptor?.annotations?.getAllAnnotations() ?: emptyList()
                var nextIndex = 0
                val result = delegate.annotations
                        .map { clsAnnotation ->
                            val slice = ktAnnotations.subList(nextIndex, ktAnnotations.size)
                            val currentIndex = slice.indexOfFirst {
                                it.annotation.type.constructor.declarationDescriptor?.fqNameUnsafe?.asString() == clsAnnotation.qualifiedName
                            }
                            if (currentIndex >= 0) {
                                nextIndex += currentIndex + 1
                                val ktAnnotation = slice[currentIndex]
                                val entry = ktAnnotation.annotation.source.getPsi() as? KtAnnotationEntry
                                KtLightAnnotation(clsAnnotation, entry, lightElement)
                            }
                            else clsAnnotation
                        }
                        .toTypedArray()

                CachedValueProvider.Result.create(result, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
            },
            false
    )
}
