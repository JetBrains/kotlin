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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractTypeAliasDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

class LazyTypeAliasDescriptor(
        private val storageManager: StorageManager,
        private val trace: BindingTrace,
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,
        name: Name,
        sourceElement: SourceElement,
        visibility: Visibility
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, sourceElement, visibility),
        TypeAliasDescriptor {

    override lateinit var underlyingType: KotlinType private set
    override lateinit var expandedType: KotlinType private set

    private val lazyTypeConstructorParameters =
            storageManager.createLazyValue { this.computeConstructorTypeParameters() }

    fun initialize(
            declaredTypeParameters: List<TypeParameterDescriptor>,
            underlyingType: KotlinType,
            expandedType: KotlinType
    ) {
        super.initialize(declaredTypeParameters)
        this.underlyingType = underlyingType
        this.expandedType = expandedType
    }

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor {
        if (substitutor.isEmpty) return this
        val substituted = LazyTypeAliasDescriptor(storageManager, trace,
                                                  containingDeclaration, annotations, name, source, visibility)
        substituted.initialize(declaredTypeParameters,
                               DeferredType.create(storageManager, trace) {
                                   substitutor.substitute(underlyingType, Variance.INVARIANT)
                               },
                               DeferredType.create(storageManager, trace) {
                                   substitutor.substitute(expandedType, Variance.INVARIANT)
                               })
        return substituted
    }

    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> =
            lazyTypeConstructorParameters()

    companion object {
        @JvmStatic fun create(
                storageManager: StorageManager,
                trace: BindingTrace,
                containingDeclaration: DeclarationDescriptor,
                annotations: Annotations,
                name: Name,
                sourceElement: SourceElement,
                visibility: Visibility
        ): LazyTypeAliasDescriptor =
                LazyTypeAliasDescriptor(storageManager, trace,
                                        containingDeclaration, annotations, name, sourceElement, visibility)
    }
}
