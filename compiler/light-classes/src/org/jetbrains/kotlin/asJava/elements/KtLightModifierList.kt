/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.fastCheckIsNullabilityApplied
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KtLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    protected val owner: T
) : KtLightElementBase(owner), PsiModifierList, KtLightElement<KtModifierList, PsiModifierList> {
    override val clsDelegate by lazyPub { owner.clsDelegate.modifierList!! }
    private val _annotations by lazyPub { computeAnnotations() }

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

    protected open fun nonSourceAnnotationsForAnnotationType(sourceAnnotations: List<PsiAnnotation>): List<KtLightAbstractAnnotation> {

        val annotations = parent.clsDelegate.modifierList?.annotations

        if (annotations.isNullOrEmpty()) return emptyList()

        return annotations.map { KtLightNonSourceAnnotation(this, it) }
    }

    private fun computeAnnotations(): List<KtLightAbstractAnnotation> {
        val annotationsForEntries = owner.givenAnnotations ?: lightAnnotationsForEntries(this)
        val modifierListOwner = parent
        if (modifierListOwner is KtLightClassForSourceDeclaration && modifierListOwner.isAnnotationType) {

            val nonSourceAnnotations = nonSourceAnnotationsForAnnotationType(annotationsForEntries)

            val filteredNonSourceAnnotations = nonSourceAnnotations.filter { nonSourceAnnotation ->
                annotationsForEntries.all { sourceAnnotation ->
                    nonSourceAnnotation.qualifiedName != sourceAnnotation.qualifiedName
                }
            }

            return annotationsForEntries + filteredNonSourceAnnotations
        }
        if ((modifierListOwner is KtLightMember<*> && modifierListOwner !is KtLightFieldImpl.KtLightEnumConstant)
            || modifierListOwner is LightParameter
        ) {

            if (fastCheckIsNullabilityApplied(modifierListOwner)) {

                val nullabilityAnnotation = when (modifierListOwner) {
                    is KtUltraLightElementWithNullabilityAnnotation<*, *> -> KtUltraLightNullabilityAnnotation(modifierListOwner, this)
                    else -> KtLightNullabilityAnnotation(modifierListOwner as KtLightElement<*, PsiModifierListOwner>, this)
                }

                return annotationsForEntries + listOf(nullabilityAnnotation)
            }
        }
        return annotationsForEntries
    }
}

class KtUltraLightSimpleModifierList(
    owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>,
    private val modifiers: Set<String>,
) : KtUltraLightModifierListBase<KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(owner) {
    override fun hasModifierProperty(name: String) = name in modifiers

    override fun copy() = KtUltraLightSimpleModifierList(owner, modifiers)
}

abstract class KtUltraLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    owner: T,
    private val support: KtUltraLightSupport
) : KtUltraLightModifierListBase<T>(owner) {

    override fun nonSourceAnnotationsForAnnotationType(sourceAnnotations: List<PsiAnnotation>): List<KtLightAbstractAnnotation> {

        if (sourceAnnotations.isEmpty()) return listOf(createRetentionRuntimeAnnotation(support, this))

        return mutableListOf<KtLightAbstractAnnotation>().also { result ->

            sourceAnnotations.mapNotNullTo(result) { sourceAnnotation ->
                sourceAnnotation.tryConvertAsTarget(support)
                    ?: sourceAnnotation.tryConvertAsRetention(support)
                    ?: sourceAnnotation.tryConvertAsMustBeDocumented(support)
            }

            if (!result.any { it.qualifiedName == "java.lang.annotation.Retention" }) {
                result.add(createRetentionRuntimeAnnotation(support, this))
            }
        }
    }
}

abstract class KtUltraLightModifierListBase<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    owner: T
) : KtLightModifierList<T>(owner) {

    override val clsDelegate: PsiModifierList get() = invalidAccess()

    private fun throwInvalidOperation(): Nothing = throw IncorrectOperationException()

    override fun setModifierProperty(name: String, value: Boolean): Unit = throwInvalidOperation()

    override fun checkSetModifierProperty(name: String, value: Boolean): Unit = throwInvalidOperation()

    override fun addAnnotation(qualifiedName: String): PsiAnnotation = throwInvalidOperation()

    override fun nonSourceAnnotationsForAnnotationType(sourceAnnotations: List<PsiAnnotation>): List<KtLightAbstractAnnotation> =
        emptyList()
}

class KtLightSimpleModifierList(
    owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>, private val modifiers: Set<String>
) : KtLightModifierList<KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(owner) {
    override fun hasModifierProperty(name: String) = name in modifiers

    override fun copy() = KtLightSimpleModifierList(owner, modifiers)
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
        .flatMap { (fqName, entries) ->
            entries.mapIndexed { index, entry ->

                val lazyClsDelegate = if (lightModifierList !is KtUltraLightModifierList) {
                    lazyPub {
                        lightModifierList.clsDelegate.annotations
                            .filter { it.qualifiedName == fqName }
                            .getOrNull(index)
                            ?: KtLightNonExistentAnnotation(lightModifierList)
                    }
                } else null

                KtLightAnnotationForSourceEntry({ fqName }, entry, lightModifierList, lazyClsDelegate)
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

private fun getAnnotationDescriptors(
    declaration: KtAnnotated,
    annotatedLightElement: KtLightElement<*, *>
): List<AnnotationDescriptor> {
    val context = LightClassGenerationSupport.getInstance(declaration.project).analyze(declaration)

    val descriptor = if (declaration is KtParameter && declaration.isPropertyParameter()) {
        if (annotatedLightElement is LightParameter && annotatedLightElement.method.isConstructor)
            context[BindingContext.VALUE_PARAMETER, declaration]
        else
            context[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, declaration]
    } else {
        context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
    }

    val annotatedDescriptor = when {
        descriptor is ClassDescriptor && annotatedLightElement is KtLightMethod && annotatedLightElement.isConstructor ->
            descriptor.unsubstitutedPrimaryConstructor
        descriptor !is PropertyDescriptor -> descriptor
        annotatedLightElement is KtLightFieldImpl.KtLightEnumConstant -> descriptor
        annotatedLightElement is KtLightField -> descriptor.backingField
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

private fun hasAnnotationsInSource(declaration: KtAnnotated): Boolean {
    if (declaration.annotationEntries.isNotEmpty()) {
        return true
    }

    if (declaration is KtProperty) {
        return declaration.accessors.any { hasAnnotationsInSource(it) }
    }

    return false
}
