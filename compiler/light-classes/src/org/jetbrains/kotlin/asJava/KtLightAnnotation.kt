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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.source.getPsi

class KtLightAnnotation(
        override val clsDelegate: PsiAnnotation,
        override val kotlinOrigin: KtAnnotationEntry,
        private val owner: PsiAnnotationOwner
) : PsiAnnotation by clsDelegate, KtLightElement<KtAnnotationEntry, PsiAnnotation> {
    inner class LightExpressionValue(private val delegate: PsiExpression) : PsiAnnotationMemberValue, PsiExpression by delegate {
        val originalExpression: PsiElement? by lazy {
            val nameAndValue = getStrictParentOfType<PsiNameValuePair>() ?: return@lazy null
            val annotationEntry = this@KtLightAnnotation.kotlinOrigin
            val context = LightClassGenerationSupport.getInstance(project).analyze(annotationEntry)
            val resolvedCall = annotationEntry.getResolvedCall(context) ?: return@lazy null
            val annotationConstructor = resolvedCall.resultingDescriptor
            val parameterName = nameAndValue.name ?: "value"
            val parameter = annotationConstructor.valueParameters.singleOrNull { it.name.asString() == parameterName }
                            ?: return@lazy null
            val resolvedArgument = resolvedCall.valueArguments[parameter] ?: return@lazy null
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
                    resolvedArgument.valueArgument?.getArgumentExpression()
                }

                is VarargValueArgument -> {
                    val arrayInitializer = parent as? PsiArrayInitializerMemberValue ?: return@lazy null
                    val exprIndex = arrayInitializer.initializers.indexOf(delegate as PsiAnnotationMemberValue)
                    if (exprIndex < 0) return@lazy null
                    resolvedArgument.arguments[exprIndex].getArgumentExpression()
                }

                else -> null
            }
        }

        override fun getReference() = references.singleOrNull()
        override fun getReferences() = ReferenceProvidersRegistry.getReferencesFromProviders(delegate, PsiReferenceService.Hints.NO_HINTS)
        override fun getLanguage() = KotlinLanguage.INSTANCE
        override fun getNavigationElement() = originalExpression
    }

    inner class LightArrayInitializerValue(private val delegate: PsiArrayInitializerMemberValue) : PsiArrayInitializerMemberValue by delegate {
        private val _initializers by lazy { delegate.initializers.map { wrapAnnotationValue(it) }.toTypedArray() }

        override fun getInitializers() = _initializers
        override fun getLanguage() = KotlinLanguage.INSTANCE
    }

    private fun wrapAnnotationValue(value: PsiAnnotationMemberValue): PsiAnnotationMemberValue {
        return when (value) {
            is PsiExpression -> LightExpressionValue(value)
            is PsiArrayInitializerMemberValue -> LightArrayInitializerValue(value)
            else -> value
        }
    }

    override fun isPhysical() = true

    override fun getName() = null
    override fun setName(newName: String) = throw IncorrectOperationException()

    override fun getOwner() = owner

    override fun findAttributeValue(name: String?) = clsDelegate.findAttributeValue(name)?.let { wrapAnnotationValue(it) }
    override fun findDeclaredAttributeValue(name: String?) = clsDelegate.findDeclaredAttributeValue(name)?.let { wrapAnnotationValue(it) }

    override fun getText() = kotlinOrigin.text ?: ""
    override fun getTextRange() = kotlinOrigin.textRange ?: TextRange.EMPTY_RANGE

    override fun getParent() = owner as? PsiElement

    override fun delete() {
        kotlinOrigin.delete()
    }

    override fun toString() = "@$qualifiedName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return kotlinOrigin == (other as KtLightAnnotation).kotlinOrigin
    }

    override fun hashCode() = kotlinOrigin.hashCode()
}
