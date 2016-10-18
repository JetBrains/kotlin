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
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.asSimpleType

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

    private lateinit var underlyingTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var expandedTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var defaultTypeImpl: NotNullLazyValue<SimpleType>

    override val underlyingType: SimpleType get() = underlyingTypeImpl()
    override val expandedType: SimpleType get() = expandedTypeImpl()
    override fun getDefaultType(): SimpleType = defaultTypeImpl()

    fun initialize(
            declaredTypeParameters: List<TypeParameterDescriptor>,
            lazyUnderlyingType: NotNullLazyValue<SimpleType>,
            lazyExpandedType: NotNullLazyValue<SimpleType>
    ) {
        super.initialize(declaredTypeParameters)
        this.underlyingTypeImpl = lazyUnderlyingType
        this.expandedTypeImpl = lazyExpandedType
        this.defaultTypeImpl = storageManager.createLazyValue { computeDefaultType() }
    }

    private val lazyTypeConstructorParameters =
            storageManager.createLazyValue { this.computeConstructorTypeParameters() }

    fun initialize(
            declaredTypeParameters: List<TypeParameterDescriptor>,
            underlyingType: SimpleType,
            expandedType: SimpleType
    ) = initialize(declaredTypeParameters, storageManager.createLazyValue { underlyingType }, storageManager.createLazyValue { expandedType })

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor {
        if (substitutor.isEmpty) return this
        val substituted = LazyTypeAliasDescriptor(storageManager, trace,
                                                  containingDeclaration, annotations, name, source, visibility)
        substituted.initialize(declaredTypeParameters,
                                   storageManager.createLazyValue {
                                       substitutor.substitute(underlyingType, Variance.INVARIANT)!!.asSimpleType()
                                   },
                                   storageManager.createLazyValue {
                                       substitutor.substitute(expandedType, Variance.INVARIANT)!!.asSimpleType()
                                   }
                               )
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
