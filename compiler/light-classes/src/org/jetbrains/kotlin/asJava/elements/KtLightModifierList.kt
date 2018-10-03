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
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME
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

    if (!isFromSources(lightModifierList)) return emptyList()

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

fun isFromSources(lightElement: KtLightElement<*, *>): Boolean {
    if (lightElement is KtLightClassForSourceDeclaration) return true
    if (lightElement.parent is KtLightClassForSourceDeclaration) return true

    val ktLightMember = lightElement.getParentOfType<KtLightMember<*>>(false) ?: return true // hope it will never happen
    if (ktLightMember.lightMemberOrigin !is LightMemberOriginForDeclaration) return false
    return true
}

private fun getAnnotationDescriptors(declaration: KtDeclaration, annotatedLightElement: KtLightElement<*, *>): List<AnnotationDescriptor> {
    val context = LightClassGenerationSupport.getInstance(declaration.project).analyze(declaration)

    val descriptor = if (declaration is KtParameter && declaration.isPropertyParameter()) {
        if (annotatedLightElement is KtLightParameter && annotatedLightElement.method.isConstructor)
            context[BindingContext.VALUE_PARAMETER, declaration]
        else
            context[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, declaration]
    }
    else {
        context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
    }

    val annotatedDescriptor = when {
        descriptor is ClassDescriptor && annotatedLightElement is KtLightMethod && annotatedLightElement.isConstructor ->
            descriptor.unsubstitutedPrimaryConstructor
        descriptor !is PropertyDescriptor -> descriptor
        annotatedLightElement is KtLightFieldImpl.KtLightFieldForDeclaration -> descriptor.backingField
        annotatedLightElement !is KtLightMethod -> descriptor
        annotatedLightElement.isGetter -> descriptor.getter
        annotatedLightElement.isSetter -> descriptor.setter
        else -> descriptor
    } ?: return emptyList()

    val annotations = annotatedDescriptor.annotations.toMutableList()

    if (descriptor is PropertyDescriptor) {
        val jvmDefault = descriptor.annotations.findAnnotation(JVM_DEFAULT_FQ_NAME)
        if (jvmDefault != null) {
            annotations.add(jvmDefault)
        }
    }

    return annotations
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
