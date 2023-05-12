/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KtLightModifierListDescriptorBasedKt")

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.KtUltraLightElementWithNullabilityAnnotation
import org.jetbrains.kotlin.asJava.classes.KtUltraLightNullabilityAnnotation
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSupport
import org.jetbrains.kotlin.asJava.fastCheckIsNullabilityApplied
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KtLightModifierListDescriptorBased<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(owner: T) :
    KtLightModifierList<T>(owner) {
    override fun computeAnnotations(): List<KtLightAbstractAnnotation> {
        val annotationsForEntries = owner.givenAnnotations ?: lightAnnotationsForEntries(this)
        //TODO: Hacky way to update wrong parents for annotations
        annotationsForEntries.forEach {
            @Suppress("DEPRECATION_ERROR")
            it.parent = this
        }
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

        if (fastCheckIsNullabilityApplied(modifierListOwner)) {

            val nullabilityAnnotation = when (modifierListOwner) {
                is KtUltraLightElementWithNullabilityAnnotation<*, *> -> KtUltraLightNullabilityAnnotation(modifierListOwner, this)
                else -> KtLightNullabilityAnnotation(modifierListOwner as KtLightElement<*, PsiModifierListOwner>, this)
            }

            return annotationsForEntries +
                    (if (nullabilityAnnotation.qualifiedName != null) listOf(nullabilityAnnotation) else emptyList())
        }

        return annotationsForEntries
    }
}

class KtUltraLightSimpleModifierList(
    owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>,
    private val modifiers: Set<String>,
) : KtLightModifierListDescriptorBased<KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(owner) {
    override fun hasModifierProperty(name: String) = name in modifiers

    override fun copy() = KtUltraLightSimpleModifierList(owner, modifiers)
}

abstract class KtUltraLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(
    owner: T,
    protected val support: KtUltraLightSupport
) : KtLightModifierListDescriptorBased<T>(owner) {

    protected open fun PsiAnnotation.additionalConverter(): KtLightAbstractAnnotation? = null

    override fun nonSourceAnnotationsForAnnotationType(sourceAnnotations: List<PsiAnnotation>): List<KtLightAbstractAnnotation> {

        if (sourceAnnotations.isEmpty()) return listOf(createRetentionRuntimeAnnotation(this))

        return mutableListOf<KtLightAbstractAnnotation>().also { result ->

            sourceAnnotations.mapNotNullTo(result) { sourceAnnotation ->
                sourceAnnotation.additionalConverter()
                    ?: sourceAnnotation.tryConvertAsTarget()
                    ?: sourceAnnotation.tryConvertAsRetention()
                    ?: sourceAnnotation.tryConvertAsRepeatable(owner)
                    ?: sourceAnnotation.tryConvertAsMustBeDocumented()
            }

            if (!result.any { it.qualifiedName == CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION }) {
                result.add(createRetentionRuntimeAnnotation(this))
            }
        }
    }
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
            entries.map { entry ->
                KtLightAnnotationForSourceEntry(
                    name = entry.shortName?.identifier,
                    lazyQualifiedName = { fqName },
                    kotlinOrigin = entry,
                    parent = lightModifierList
                )
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
