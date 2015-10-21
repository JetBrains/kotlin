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

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.types.TypeProjectionImpl


interface TypeBinding<out P : PsiElement> {
    val psiElement: P
    val jetType: KotlinType
    fun getArgumentBindings(): List<TypeArgumentBinding<P>?>
}

interface TypeArgumentBinding<out P: PsiElement> {
    val typeProjection: TypeProjection
    val typeParameterDescriptor: TypeParameterDescriptor?
    val typeBinding: TypeBinding<P>
}

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
    val jetTypeReference = getTypeReference()
    if (jetTypeReference != null) return jetTypeReference.createTypeBinding(trace)

    val descriptor = trace[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    if (descriptor !is CallableDescriptor) return null

    return descriptor.getReturnType()?.let { NoTypeElementBinding(trace, this, it) }
}

private class TypeArgumentBindingImpl<out P: PsiElement>(
        override val typeProjection: TypeProjection,
        override val typeParameterDescriptor: TypeParameterDescriptor?,
        override val typeBinding: TypeBinding<P>
) : TypeArgumentBinding<P>

private class ExplicitTypeBinding(
        private val trace: BindingContext,
        override val psiElement: KtTypeElement,
        override val jetType: KotlinType
) : TypeBinding<KtTypeElement> {

    override fun getArgumentBindings(): List<TypeArgumentBinding<KtTypeElement>?> {
        val psiTypeArguments = psiElement.getTypeArgumentsAsTypes()
        val isErrorBinding = run {
            val sizeIsEqual = psiTypeArguments.size() == jetType.getArguments().size()
                              && psiTypeArguments.size() == jetType.getConstructor().getParameters().size()
            jetType.isError() || !sizeIsEqual
        }

        return psiTypeArguments.indices.map { index: Int ->
            // todo fix for List<*>
            val jetTypeReference = psiTypeArguments[index]
            val jetTypeElement = jetTypeReference?.typeElement
            if (jetTypeElement == null) return@map null;

            if (isErrorBinding) {
                val nextJetType = trace[BindingContext.TYPE, jetTypeReference]
                if (nextJetType == null) return@map null;

                return@map TypeArgumentBindingImpl(
                        TypeProjectionImpl(nextJetType),
                        null,
                        ExplicitTypeBinding(trace, jetTypeElement, nextJetType)
                )
            }

            val typeProjection = jetType.getArguments()[index]
            return@map TypeArgumentBindingImpl(
                    typeProjection,
                    jetType.getConstructor().getParameters()[index],
                    ExplicitTypeBinding(trace, jetTypeElement, typeProjection.getType())
            )
        }
    }
}

private class NoTypeElementBinding<out P : PsiElement>(
        private val trace: BindingContext,
        override val psiElement: P,
        override val jetType: KotlinType
): TypeBinding<P> {

    override fun getArgumentBindings(): List<TypeArgumentBinding<P>?> {
        val isErrorBinding = jetType.isError() || jetType.getConstructor().getParameters().size() != jetType.getArguments().size()
        return jetType.getArguments().indices.map {
            val typeProjection = jetType.getArguments()[it]
            TypeArgumentBindingImpl(
                    typeProjection,
                    if (isErrorBinding) null else jetType.getConstructor().getParameters()[it],
                    NoTypeElementBinding(trace, psiElement, typeProjection.getType())
            )
        }
    }
}

