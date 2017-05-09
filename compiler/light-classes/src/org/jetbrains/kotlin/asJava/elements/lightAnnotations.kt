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

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeUtils

abstract class KtLightAbstractAnnotation(parent: PsiElement, computeDelegate: () -> PsiAnnotation) :
        KtLightElementBase(parent), PsiAnnotation, KtLightElement<KtAnnotationEntry, PsiAnnotation> {
    override val clsDelegate by lazyPub(computeDelegate)

    override fun getNameReferenceElement() = clsDelegate.nameReferenceElement

    override fun getOwner() = parent as? PsiAnnotationOwner

    override fun getMetaData() = clsDelegate.metaData

    override fun getParameterList() = clsDelegate.parameterList

    open fun fqNameMatches(fqName: String): Boolean = qualifiedName == fqName
}

class KtLightAnnotationForSourceEntry(
        private val qualifiedName: String,
        override val kotlinOrigin: KtAnnotationEntry,
        parent: KtLightElement<*, *>,
        computeDelegate: () -> PsiAnnotation
) : KtLightAbstractAnnotation(parent, computeDelegate) {

    override fun getQualifiedName() = qualifiedName

    open inner class LightExpressionValue<out D : PsiExpression>(
            val delegate: D,
            private val parent: PsiElement
    ) : PsiAnnotationMemberValue, PsiExpression by delegate {
        val originalExpression: PsiElement? by lazyPub {
            val nameAndValue = delegate.getStrictParentOfType<PsiNameValuePair>() ?: return@lazyPub null
            val annotationEntry = this@KtLightAnnotationForSourceEntry.kotlinOrigin
            val context = LightClassGenerationSupport.getInstance(project).analyze(annotationEntry)
            val resolvedCall = annotationEntry.getResolvedCall(context) ?: return@lazyPub null
            val annotationConstructor = resolvedCall.resultingDescriptor
            val parameterName = nameAndValue.name ?: "value"
            val parameter = annotationConstructor.valueParameters.singleOrNull { it.name.asString() == parameterName }
                            ?: return@lazyPub null
            val resolvedArgument = resolvedCall.valueArguments[parameter] ?: return@lazyPub null
            when (resolvedArgument) {
                is DefaultValueArgument -> {
                    val psi = parameter.source.getPsi()
                    when (psi) {
                        is KtParameter -> psi.defaultValue
                        is PsiAnnotationMethod -> psi.defaultValue
                        else -> null
                    }
                }

                is ExpressionValueArgument -> {
                    val argExpression = resolvedArgument.valueArgument?.getArgumentExpression()
                    // arrayOf()
                    if (argExpression is KtCallExpression) unwrapArray(argExpression.valueArguments) else argExpression
                }

                is VarargValueArgument -> unwrapArray(resolvedArgument.arguments)

                else -> null
            }
        }

        fun getConstantValue(): Any? {
            val expression = originalExpression as? KtExpression ?: return null
            val annotationEntry = this@KtLightAnnotationForSourceEntry.kotlinOrigin
            val context = LightClassGenerationSupport.getInstance(project).analyze(annotationEntry)
            return context[BindingContext.COMPILE_TIME_VALUE, expression]?.getValue(TypeUtils.NO_EXPECTED_TYPE)
        }

        private fun unwrapArray(arguments: List<ValueArgument>): PsiElement? {
            val arrayInitializer = parent as? LightArrayInitializerValue ?: return null
            val exprIndex = arrayInitializer.initializers.indexOf(this)
            if (exprIndex < 0 || exprIndex >= arguments.size) return null
            return arguments[exprIndex].getArgumentExpression()
        }

        override fun getReference() = references.singleOrNull()
        override fun getReferences() = originalExpression?.references ?: PsiReference.EMPTY_ARRAY
        override fun getLanguage() = KotlinLanguage.INSTANCE
        override fun getNavigationElement() = originalExpression
        override fun getTextRange() = originalExpression?.textRange ?: TextRange.EMPTY_RANGE
        override fun getParent() = parent

        override fun replace(newElement: PsiElement): PsiElement {
            val value = (newElement as? PsiLiteral)?.value as? String ?: return this
            val origin = originalExpression ?: return this

            val exprToReplace =
                    if (origin is KtCallExpression /*arrayOf*/) {
                        unwrapArray(origin.valueArguments)
                    }
                    else {
                        origin as? KtExpression
                    } ?: return this
            exprToReplace.replace(KtPsiFactory(this).createExpression("\"${StringUtil.escapeStringCharacters(value)}\""))

            return this
        }
    }

    inner class LightStringLiteral(
            delegate: PsiLiteralExpression,
            parent: PsiElement
    ) : LightExpressionValue<PsiLiteralExpression>(delegate, parent), PsiLiteralExpression {
        override fun getValue() = delegate.value
    }

    inner class LightClassLiteral(
            delegate: PsiClassObjectAccessExpression,
            parent: PsiElement
    ) : LightExpressionValue<PsiClassObjectAccessExpression>(delegate, parent), PsiClassObjectAccessExpression {
        override fun getType() = delegate.type
        override fun getOperand(): PsiTypeElement = delegate.operand
    }

    inner class LightArrayInitializerValue(
            private val delegate: PsiArrayInitializerMemberValue,
            private val parent: PsiElement
    ) : PsiArrayInitializerMemberValue by delegate {
        private val _initializers by lazyPub { delegate.initializers.map { wrapAnnotationValue(it, this) }.toTypedArray() }

        override fun getInitializers() = _initializers
        override fun getLanguage() = KotlinLanguage.INSTANCE
        override fun getParent() = parent
    }

    private fun wrapAnnotationValue(value: PsiAnnotationMemberValue, parent: PsiElement): PsiAnnotationMemberValue {
        return when {
            value is PsiLiteralExpression && value.value is String -> LightStringLiteral(value, parent)
            value is PsiClassObjectAccessExpression -> LightClassLiteral(value, parent)
            value is PsiExpression -> LightExpressionValue(value, parent)
            value is PsiArrayInitializerMemberValue -> LightArrayInitializerValue(value, parent)
            else -> value
        }
    }

    override fun isPhysical() = true

    override fun getName() = null

    override fun findAttributeValue(name: String?) = clsDelegate.findAttributeValue(name)?.let { wrapAnnotationValue(it, this) }
    override fun findDeclaredAttributeValue(name: String?) = clsDelegate.findDeclaredAttributeValue(name)?.let { wrapAnnotationValue(it, this) }

    override fun delete() = kotlinOrigin.delete()

    override fun toString() = "@$qualifiedName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return kotlinOrigin == (other as KtLightAnnotationForSourceEntry).kotlinOrigin
    }

    override fun hashCode() = kotlinOrigin.hashCode()

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()
}

class KtLightNonSourceAnnotation(
        parent: PsiElement, clsDelegate: PsiAnnotation
): KtLightAbstractAnnotation(parent, { clsDelegate }) {
    override val kotlinOrigin: KtAnnotationEntry? get() = null
    override fun getQualifiedName() = clsDelegate.qualifiedName
    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()
    override fun findAttributeValue(attributeName: String?) = clsDelegate.findAttributeValue(attributeName)
    override fun findDeclaredAttributeValue(attributeName: String?) = clsDelegate.findDeclaredAttributeValue(attributeName)
}

class KtLightNonExistentAnnotation(parent: KtLightElement<*, *>) : KtLightElementBase(parent), PsiAnnotation {
    override val kotlinOrigin get() = null
    override fun toString() = this.javaClass.name

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun getNameReferenceElement() = null
    override fun findAttributeValue(attributeName: String?) = null
    override fun getQualifiedName() = null
    override fun getOwner() = parent as? PsiAnnotationOwner
    override fun findDeclaredAttributeValue(attributeName: String?) = null
    override fun getMetaData() = null
    override fun getParameterList() = KtLightEmptyAnnotationParameterList(this)
}

class KtLightEmptyAnnotationParameterList(parent: PsiElement) : KtLightElementBase(parent), PsiAnnotationParameterList {
    override val kotlinOrigin get() = null
    override fun getAttributes(): Array<PsiNameValuePair> = emptyArray()
}

class KtLightNullabilityAnnotation(member: KtLightElement<*, PsiModifierListOwner>, parent: PsiElement) : KtLightAbstractAnnotation(parent, {
    // searching for last because nullability annotations are generated after backend generates source annotations
    member.clsDelegate.modifierList?.annotations?.findLast {
        isNullabilityAnnotation(it.qualifiedName)
    } ?: KtLightNonExistentAnnotation(member)
}) {
    override fun fqNameMatches(fqName: String): Boolean {
        if (!isNullabilityAnnotation(fqName)) return false

        return super.fqNameMatches(fqName)
    }

    override val kotlinOrigin get() = null
    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun findAttributeValue(attributeName: String?) = null

    override fun getQualifiedName(): String? = clsDelegate.qualifiedName

    override fun findDeclaredAttributeValue(attributeName: String?) = null
}

internal fun isNullabilityAnnotation(qualifiedName: String?) = qualifiedName in backendNullabilityAnnotations

private val backendNullabilityAnnotations = arrayOf(Nullable::class.java.name, NotNull::class.java.name)