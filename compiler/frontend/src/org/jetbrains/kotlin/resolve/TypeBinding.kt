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

package org.jetbrains.kotlin.resolve.typeBinding

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.*


interface TypeBinding<out P : PsiElement> : TypeHolder<TypeBinding<P>> {
    val psiElement: P
    override val arguments: List<TypeArgumentBinding<P>?>
}

interface TypeArgumentBinding<out P: PsiElement> : TypeHolderArgument<TypeBinding<P>>

fun KtTypeReference.createTypeBinding(trace: BindingContext): TypeBinding<KtTypeElement>? {
    val jetType = trace[BindingContext.TYPE, this]
    val psiElement = typeElement
    if (jetType == null || psiElement == null) {
        return null
    }
    else {
        return ExplicitTypeBinding(trace, psiElement, jetType)
    }
}

fun KtCallableDeclaration.createTypeBindingForReturnType(trace: BindingContext): TypeBinding<PsiElement>?  {
    val jetTypeReference = typeReference
    if (jetTypeReference != null) return jetTypeReference.createTypeBinding(trace)

    val descriptor = trace[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    if (descriptor !is CallableDescriptor) return null

    return descriptor.returnType?.let { NoTypeElementBinding(trace, this, it) }
}

private class TypeArgumentBindingImpl<out P: PsiElement>(
        override val projection: TypeProjection,
        override val typeParameter: TypeParameterDescriptor?,
        override val holder: TypeBinding<P>
) : TypeArgumentBinding<P>

private class ExplicitTypeBinding(
        private val trace: BindingContext,
        override val psiElement: KtTypeElement,
        override val type: KotlinType
) : TypeBinding<KtTypeElement> {

    override val arguments: List<TypeArgumentBinding<KtTypeElement>?>
        get() {
            val psiTypeArguments = psiElement.typeArgumentsAsTypes
            val isErrorBinding = run {
                val sizeIsEqual = psiTypeArguments.size == type.arguments.size
                                  && psiTypeArguments.size == type.constructor.parameters.size
                type.isError || !sizeIsEqual
            }

        return psiTypeArguments.indices.map { index: Int ->
            // todo fix for List<*>
            val jetTypeReference = psiTypeArguments[index]
            val jetTypeElement = jetTypeReference?.typeElement
            if (jetTypeElement == null) return@map null

            if (isErrorBinding) {
                val nextJetType = trace[BindingContext.TYPE, jetTypeReference]
                if (nextJetType == null) return@map null

                    return@map TypeArgumentBindingImpl(
                            TypeProjectionImpl(nextJetType),
                            null,
                            ExplicitTypeBinding(trace, jetTypeElement, nextJetType)
                    )
                }

                val typeProjection = type.arguments[index]
                return@map TypeArgumentBindingImpl(
                        typeProjection,
                        type.constructor.parameters[index],
                        ExplicitTypeBinding(trace, jetTypeElement, typeProjection.type)
                )
            }
        }
}

private class NoTypeElementBinding<out P : PsiElement>(
        private val trace: BindingContext,
        override val psiElement: P,
        override val type: KotlinType
): TypeBinding<P> {

    override val arguments: List<TypeArgumentBinding<P>?>
        get() {
            val isErrorBinding = type.isError || type.constructor.parameters.size != type.arguments.size
            return type.arguments.indices.map {
                val typeProjection = type.arguments[it]
                TypeArgumentBindingImpl(
                        typeProjection,
                        if (isErrorBinding) null else type.constructor.parameters[it],
                        NoTypeElementBinding(trace, psiElement, typeProjection.type)
                )
            }
        }
}

