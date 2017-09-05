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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeUtils

abstract class KtLightAbstractAnnotation(parent: PsiElement, computeDelegate: () -> PsiAnnotation) :
        KtLightElementBase(parent), PsiAnnotation, KtLightElement<KtCallElement, PsiAnnotation> {
    override val clsDelegate by lazyPub(computeDelegate)

    override fun getNameReferenceElement() = clsDelegate.nameReferenceElement

    override fun getOwner() = parent as? PsiAnnotationOwner

    override fun getMetaData() = clsDelegate.metaData

    override fun getParameterList() = clsDelegate.parameterList

    override fun canNavigate(): Boolean = super<KtLightElementBase>.canNavigate()

    override fun canNavigateToSource(): Boolean = super<KtLightElementBase>.canNavigateToSource()

    override fun navigate(requestFocus: Boolean) = super<KtLightElementBase>.navigate(requestFocus)

    open fun fqNameMatches(fqName: String): Boolean = qualifiedName == fqName
}

private typealias AnnotationValueOrigin = () -> PsiElement?

class KtLightAnnotationForSourceEntry(
        private val qualifiedName: String,
        override val kotlinOrigin: KtCallElement,
        parent: PsiElement,
        computeDelegate: () -> PsiAnnotation
) : KtLightAbstractAnnotation(parent, computeDelegate) {

    override fun getQualifiedName() = qualifiedName

    open inner class LightElementValue<out D : PsiElement>(
            val delegate: D,
            private val parent: PsiElement,
            valueOrigin: AnnotationValueOrigin
    ) : PsiAnnotationMemberValue, PsiCompiledElement, PsiElement by delegate {
        override fun getMirror(): PsiElement = delegate

        val originalExpression: PsiElement? by lazyPub(valueOrigin)

        fun getConstantValue(): Any? {
            val expression = originalExpression as? KtExpression ?: return null
            val annotationEntry = this@KtLightAnnotationForSourceEntry.kotlinOrigin
            val context = LightClassGenerationSupport.getInstance(project).analyze(annotationEntry)
            return context[BindingContext.COMPILE_TIME_VALUE, expression]?.getValue(TypeUtils.NO_EXPECTED_TYPE)
        }

        override fun getReference() = references.singleOrNull()
        override fun getReferences() = originalExpression?.references.orEmpty()
        override fun getLanguage() = KotlinLanguage.INSTANCE
        override fun getNavigationElement() = originalExpression
        override fun isPhysical(): Boolean = originalExpression?.containingFile == kotlinOrigin.containingFile
        override fun getTextRange() = originalExpression?.textRange ?: TextRange.EMPTY_RANGE
        override fun getParent() = parent
        override fun getText() = originalExpression?.text.orEmpty()
        override fun getContainingFile(): PsiFile? = if (isPhysical) kotlinOrigin.containingFile else delegate.containingFile

        override fun replace(newElement: PsiElement): PsiElement {
            val value = (newElement as? PsiLiteral)?.value as? String ?: return this
            val origin = originalExpression

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

    private fun getMemberValueAsCallArgument(memberValue: PsiElement, callHolder: KtCallElement): PsiElement? {
        val resolvedCall = callHolder.getResolvedCall() ?: return null
        val annotationConstructor = resolvedCall.resultingDescriptor
        val parameterName =
                memberValue.getNonStrictParentOfType<PsiNameValuePair>()?.name ?:
                memberValue.getNonStrictParentOfType<PsiAnnotationMethod>()?.takeIf { it.containingClass?.isAnnotationType == true }?.name ?:
                "value"

        val parameter = annotationConstructor.valueParameters.singleOrNull { it.name.asString() == parameterName } ?: return null
        val resolvedArgument = resolvedCall.valueArguments[parameter] ?: return null
        return when (resolvedArgument) {
            is DefaultValueArgument -> {
                val psi = parameter.source.getPsi()
                when (psi) {
                    is KtParameter -> psi.defaultValue
                    is PsiAnnotationMethod -> psi.defaultValue
                    else -> error("$psi of type ${psi?.javaClass}")
                }
            }

            is ExpressionValueArgument -> {
                val argExpression = resolvedArgument.valueArgument?.getArgumentExpression()
                argExpression?.asKtCall()
                ?: argExpression
                ?: error("resolvedArgument ($resolvedArgument) has no arg expression")
            }

            is VarargValueArgument ->
                memberValue.unwrapArray(resolvedArgument.arguments)
                ?: resolvedArgument.arguments.first().asElement().parent.parent.let {
                    it.asKtCall() ?: it
                }

            else -> error("resolvedArgument: ${resolvedArgument.javaClass} cant be processed")
        }
    }

    private fun PsiElement.unwrapArray(arguments: List<ValueArgument>): PsiElement? {
        val arrayInitializer = parent as? PsiArrayInitializerMemberValue ?: return null
        val exprIndex = arrayInitializer.initializers.indexOf(this)
        if (exprIndex < 0 || exprIndex >= arguments.size) return null
        return arguments[exprIndex].getArgumentExpression()
    }

    open inner class LightExpressionValue<out D : PsiExpression>(
            delegate: D,
            parent: PsiElement,
            valueOrigin: AnnotationValueOrigin
    ) : LightElementValue<D>(delegate, parent, valueOrigin), PsiExpression {
        override fun getType(): PsiType? = delegate.type
    }

    inner class LightStringLiteral(
            delegate: PsiLiteralExpression,
            parent: PsiElement,
            valueOrigin: AnnotationValueOrigin
    ) : LightExpressionValue<PsiLiteralExpression>(delegate, parent, valueOrigin), PsiLiteralExpression {
        override fun getValue() = delegate.value
    }

    inner class LightClassLiteral(
            delegate: PsiClassObjectAccessExpression,
            parent: PsiElement,
            valueOrigin: AnnotationValueOrigin
    ) : LightExpressionValue<PsiClassObjectAccessExpression>(delegate, parent, valueOrigin), PsiClassObjectAccessExpression {
        override fun getType() = delegate.type
        override fun getOperand(): PsiTypeElement = delegate.operand
    }

    inner class LightArrayInitializerValue(
            delegate: PsiArrayInitializerMemberValue,
            parent: PsiElement,
            valueOrigin: AnnotationValueOrigin
    ) : LightElementValue<PsiArrayInitializerMemberValue>(delegate, parent, valueOrigin), PsiArrayInitializerMemberValue {
        private val _initializers by lazyPub {
            delegate.initializers.mapIndexed { i, it ->
                wrapAnnotationValue(it, this, {
                    originalExpression.let {
                        when (it) {
                            is KtCallElement -> it.valueArguments[i].getArgumentExpression()!!
                            is KtCollectionLiteralExpression -> it.getInnerExpressions()[i]
                            else -> throw UnsupportedOperationException("cant process $it of type ${it?.javaClass}")
                        }
                    }
                })
            }.toTypedArray()
        }

        override fun getInitializers() = _initializers
    }

    private fun wrapAnnotationValue(value: PsiAnnotationMemberValue, parent: PsiElement, ktOrigin: AnnotationValueOrigin): PsiAnnotationMemberValue {
        return when {
            value is PsiLiteralExpression && value.value is String -> LightStringLiteral(value, parent, ktOrigin)
            value is PsiClassObjectAccessExpression -> LightClassLiteral(value, parent, ktOrigin)
            value is PsiExpression -> LightExpressionValue(value, parent, ktOrigin)
            value is PsiArrayInitializerMemberValue -> LightArrayInitializerValue(value, parent, ktOrigin)
            value is PsiAnnotation -> KtLightAnnotationForSourceEntry(
                    value.qualifiedName!!,
                    ktOrigin().let {
                        it?.asKtCall() ?: throw UnsupportedOperationException("cant convert $it to KtCallElement")
                    },
                    parent, { value }
            )
            else -> LightElementValue(value, parent, ktOrigin)
        }
    }

    override fun isPhysical() = true

    override fun getName() = null

    private fun wrapAnnotationValue(value: PsiAnnotationMemberValue): PsiAnnotationMemberValue = wrapAnnotationValue(value, this, {
        getMemberValueAsCallArgument(value, kotlinOrigin)
    })

    override fun findAttributeValue(name: String?) = clsDelegate.findAttributeValue(name)?.let { wrapAnnotationValue(it) }

    override fun findDeclaredAttributeValue(name: String?) = clsDelegate.findDeclaredAttributeValue(name)?.let { wrapAnnotationValue(it) }

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
) : KtLightAbstractAnnotation(parent, { clsDelegate }) {
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

    override fun canNavigate(): Boolean = super<KtLightElementBase>.canNavigate()

    override fun canNavigateToSource(): Boolean = super<KtLightElementBase>.canNavigateToSource()

    override fun navigate(requestFocus: Boolean) = super<KtLightElementBase>.navigate(requestFocus)
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

private fun KtElement.getResolvedCall(): ResolvedCall<out CallableDescriptor>? {
    val context = LightClassGenerationSupport.getInstance(this.project).analyze(this)
    return this.getResolvedCall(context)
}

private fun PsiElement.asKtCall(): KtCallElement? = (this as? KtElement)?.getResolvedCall()?.call?.callElement as? KtCallElement