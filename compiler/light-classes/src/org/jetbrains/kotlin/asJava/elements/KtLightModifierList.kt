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

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KtLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(protected val owner: T)
    : KtLightElementBase(owner), PsiModifierList, KtLightElement<KtModifierList, PsiModifierList> {
    override val clsDelegate by lazyPub { owner.clsDelegate.modifierList!! }
    private val _annotations by lazyPub { computeAnnotations(this) }

    override val kotlinOrigin: KtModifierList?
        get() = owner.kotlinOrigin?.modifierList

    override fun getParent() = owner

    override fun hasExplicitModifier(name: String) = hasModifierProperty(name)

    override fun setModifierProperty(name: String, value: Boolean) = clsDelegate.setModifierProperty(name, value)
    override fun checkSetModifierProperty(name: String, value: Boolean) = clsDelegate.checkSetModifierProperty(name, value)
    override fun addAnnotation(qualifiedName: String) = clsDelegate.addAnnotation(qualifiedName)

    override fun getApplicableAnnotations(): Array<out PsiAnnotation> = annotations

    override fun getAnnotations(): Array<out PsiAnnotation> = _annotations.toTypedArray()
    override fun findAnnotation(qualifiedName: String) = _annotations.firstOrNull { it.fqNameMatches(qualifiedName) }

    override fun isEquivalentTo(another: PsiElement?) =
            another is KtLightModifierList<*> && owner == another.owner

    override fun isWritable() = false

    override fun toString() = "Light modifier list of $owner"
}

class KtLightSimpleModifierList(
        owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>, private val modifiers: Set<String>
) : KtLightModifierList<KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(owner) {
    override fun hasModifierProperty(name: String) = name in modifiers

    override fun copy() = KtLightSimpleModifierList(owner, modifiers)
}

private fun computeAnnotations(lightModifierList: KtLightModifierList<*>): List<KtLightAbstractAnnotation> {
    val annotationsForEntries = lightAnnotationsForEntries(lightModifierList)
    val modifierListOwner = lightModifierList.parent
    if (modifierListOwner is KtLightClassForSourceDeclaration && modifierListOwner.isAnnotationType) {
        val sourceAnnotationNames = annotationsForEntries.mapTo(mutableSetOf()) { it.qualifiedName }
        val specialAnnotationsOnAnnotationClass = modifierListOwner.clsDelegate.modifierList?.annotations.orEmpty().filter {
            it.qualifiedName !in sourceAnnotationNames
        }.map { KtLightNonSourceAnnotation(lightModifierList, it) }
        return annotationsForEntries + specialAnnotationsOnAnnotationClass
    }
    if ((modifierListOwner is KtLightMember<*> && modifierListOwner !is KtLightFieldImpl.KtLightEnumConstant)
        || modifierListOwner is KtLightParameter) {
        return annotationsForEntries +
               @Suppress("UNCHECKED_CAST")
               listOf(KtLightNullabilityAnnotation(modifierListOwner as KtLightElement<*, PsiModifierListOwner>, lightModifierList))
    }
    return annotationsForEntries
}

private fun lightAnnotationsForEntries(lightModifierList: KtLightModifierList<*>): List<KtLightAnnotationForSourceEntry> {
    val lightModifierListOwner = lightModifierList.parent
    val annotatedKtDeclaration = lightModifierListOwner.kotlinOrigin as? KtDeclaration

    if (annotatedKtDeclaration == null || !annotatedKtDeclaration.isValid || !hasAnnotationsInSource(annotatedKtDeclaration)) {
        return emptyList()
    }

    return getAnnotationDescriptors(annotatedKtDeclaration, lightModifierListOwner)
            .mapNotNull { descriptor ->
                val fqName = descriptor.fqName?.asString() ?: return@mapNotNull null
                val entry = descriptor.source.getPsi() as? KtAnnotationEntry ?: return@mapNotNull null
                Pair(fqName, entry)
            }
            .groupBy({ it.first }) { it.second }
            .flatMap {
                (fqName, entries) ->
                entries.mapIndexed { index, entry ->
                    KtLightAnnotationForSourceEntry(fqName, entry, lightModifierList) {
                        lightModifierList.clsDelegate.annotations.filter { it.qualifiedName == fqName }.getOrNull(index)
                        ?: KtLightNonExistentAnnotation(lightModifierList)
                    }
                }
            }
}

private fun getAnnotationDescriptors(declaration: KtDeclaration?, annotatedLightElement: KtLightElement<*, *>): List<AnnotationDescriptor> {
    val descriptor = declaration?.let { LightClassGenerationSupport.getInstance(it.project).resolveToDescriptor(it) }
    val annotatedDescriptor = when {
        descriptor is ClassDescriptor && annotatedLightElement is KtLightMethod && annotatedLightElement.isConstructor -> descriptor.unsubstitutedPrimaryConstructor
        descriptor !is PropertyDescriptor || annotatedLightElement !is KtLightMethod -> descriptor
        annotatedLightElement.isGetter -> descriptor.getter
        annotatedLightElement.isSetter -> descriptor.setter
        else -> descriptor
    } ?: return emptyList()

    return annotatedDescriptor.annotations.getAllAnnotations().
            filter { it.matches(annotatedLightElement) }.
            map { it.annotation }
}

private fun hasAnnotationsInSource(declaration: KtDeclaration): Boolean {
    if (declaration.annotationEntries.isNotEmpty()) {
        return true
    }

    if (declaration is KtProperty) {
        return declaration.accessors.any { hasAnnotationsInSource(it) }
    }

    return false
}

private fun AnnotationWithTarget.matches(annotated: KtLightElement<*, *>): Boolean {
    if (annotated !is KtLightFieldImpl.KtLightFieldForDeclaration) return true

    if (target == AnnotationUseSiteTarget.FIELD) return true

    if (target != null) return false

    val declarationSiteTargets = AnnotationChecker.applicableTargetSet(annotation)
    return KotlinTarget.FIELD in declarationSiteTargets && KotlinTarget.PROPERTY !in declarationSiteTargets
}