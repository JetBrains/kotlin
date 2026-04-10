/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement

internal class BinaryDelegateAnnotationsProvider(
    private val delegateProvider: () -> PsiModifierListOwner?,
) : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        delegateProvider()?.modifierList?.annotations
            ?.mapNotNull { annotation -> annotation.toSupportedBinaryDelegateAnnotation(owner) }
            ?.forEach { annotation ->
                currentRawAnnotations += annotation
                annotation.qualifiedName?.let(foundQualifiers::add)
            }
    }

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false

    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = null
}

private fun PsiAnnotation.toSupportedBinaryDelegateAnnotation(owner: PsiElement): PsiAnnotation? = when (qualifiedName) {
    JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION.asString(),
    JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION.asString(),
        -> SymbolLightSimpleAnnotation(qualifiedName, owner)

    StandardNames.FqNames.mustBeDocumented.asString(),
    StandardNames.FqNames.repeatable.asString(),
    StandardNames.FqNames.retention.asString(),
    StandardNames.FqNames.target.asString(),
    JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
    JvmStandardClassIds.JVM_NAME.asString(),
    JvmAnnotationNames.REPEATABLE_ANNOTATION.asString(),
    JvmAnnotationNames.RETENTION_ANNOTATION.asString(),
    JvmAnnotationNames.TARGET_ANNOTATION.asString(),
        -> BinaryDelegateLightAnnotation(this, owner)

    else -> null
}

internal class DeduplicatingAnnotationFilter(
    private val delegate: AnnotationFilter,
) : AnnotationFilter {
    override fun isAllowed(qualifiedName: String): Boolean = delegate.isAllowed(qualifiedName)

    override fun filtered(annotations: Collection<PsiAnnotation>): Collection<PsiAnnotation> {
        val seenQualifiers = hashSetOf<String>()
        val result = mutableListOf<PsiAnnotation>()

        for (annotation in delegate.filtered(annotations).toList().asReversed()) {
            val qualifiedName = annotation.qualifiedName
            if (qualifiedName == null || seenQualifiers.add(qualifiedName)) {
                result += annotation
            }
        }

        result.reverse()
        return result
    }
}

private class BinaryDelegateLightAnnotation(
    private val delegate: PsiAnnotation,
    parent: PsiElement,
) : SymbolLightAbstractAnnotation(parent) {
    override fun createReferenceInformationProvider(): ReferenceInformationProvider = ReferenceInformationHolder(
        referenceName = delegate.qualifiedName?.substringAfterLast('.'),
    )

    override val kotlinOrigin: KtCallElement? get() = null

    override fun getQualifiedName(): String? = delegate.qualifiedName

    override fun getName(): String? = delegate.qualifiedName

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        BinaryDelegateAnnotationParameterList(this, delegate.parameterList)
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

    override fun equals(other: Any?): Boolean = this === other ||
            other is BinaryDelegateLightAnnotation &&
            other.delegate == delegate &&
            other.parent == parent

    override fun hashCode(): Int = 31 * delegate.hashCode() + parent.hashCode()
}

private class BinaryDelegateAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
    private val delegate: PsiAnnotationParameterList,
) : SymbolLightAbstractAnnotationParameterList(parent) {
    private val _attributes: Array<PsiNameValuePair> by lazyPub {
        delegate.attributes.map { attribute -> BinaryDelegateNameValuePair(attribute, this) }
            .toArrayIfNotEmptyOrDefault(PsiNameValuePair.EMPTY_ARRAY)
    }

    override fun getAttributes(): Array<PsiNameValuePair> = _attributes
}

private class BinaryDelegateNameValuePair(
    private val delegate: PsiNameValuePair,
    parent: PsiAnnotationParameterList,
) : KtLightElementBase(parent), PsiNameValuePair {
    override val kotlinOrigin: KtElement? get() = null

    private val _value: PsiAnnotationMemberValue? by lazyPub {
        delegate.value?.toBinaryDelegateAnnotationMemberValue(this)
    }

    private val _nameIdentifier: PsiIdentifier by lazyPub {
        LightIdentifier(manager, name)
    }

    override fun setValue(newValue: PsiAnnotationMemberValue) = cannotModify()

    override fun getNameIdentifier(): PsiIdentifier = _nameIdentifier

    override fun getValue(): PsiAnnotationMemberValue? = _value

    override fun getLiteralValue(): String? = (value as? PsiLiteralExpression)?.value?.toString()

    override fun getName(): String = delegate.name ?: PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitNameValuePair(this)
        } else {
            visitor.visitElement(this)
        }
    }
}

private fun PsiAnnotationMemberValue.toBinaryDelegateAnnotationMemberValue(parent: PsiElement): PsiAnnotationMemberValue? = when (this) {
    is PsiArrayInitializerMemberValue -> SymbolPsiArrayInitializerMemberValue(null, parent) { arrayParent ->
        initializers.mapNotNull { value -> value.toBinaryDelegateAnnotationMemberValue(arrayParent) }
    }

    is PsiAnnotation -> BinaryDelegateLightAnnotation(this, parent)

    is PsiClassObjectAccessExpression -> SymbolPsiClassObjectAccessExpression(null, parent, operand.type)

    is PsiLiteralExpression -> SymbolPsiLiteral(null, parent, this)

    is PsiReferenceExpression -> SymbolPsiReference(null, parent, this)

    is PsiExpression -> SymbolPsiExpression(null, parent, this)

    else -> null
}
