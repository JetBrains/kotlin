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
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.*

class LazyTypeAliasDescriptor(
        override val storageManager: StorageManager,
        private val trace: BindingTrace,
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,
        name: Name,
        sourceElement: SourceElement,
        visibility: Visibility
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, sourceElement, visibility),
        TypeAliasDescriptor {
    override val constructors: Collection<TypeAliasConstructorDescriptor> by storageManager.createLazyValue {
        getTypeAliasConstructors()
    }

    private lateinit var underlyingTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var expandedTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var defaultTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var classDescriptorImpl: NullableLazyValue<ClassDescriptor>
    private val isImpl = (source.getPsi() as? KtTypeAlias)?.hasModifier(KtTokens.IMPL_KEYWORD) == true

    override val underlyingType: SimpleType get() = underlyingTypeImpl()
    override val expandedType: SimpleType get() = expandedTypeImpl()
    override val classDescriptor: ClassDescriptor? get() = classDescriptorImpl()
    override fun getDefaultType(): SimpleType = defaultTypeImpl()

    override fun isImpl(): Boolean = isImpl

    fun initialize(
            declaredTypeParameters: List<TypeParameterDescriptor>,
            lazyUnderlyingType: NotNullLazyValue<SimpleType>,
            lazyExpandedType: NotNullLazyValue<SimpleType>
    ) {
        super.initialize(declaredTypeParameters)
        this.underlyingTypeImpl = lazyUnderlyingType
        this.expandedTypeImpl = lazyExpandedType
        this.defaultTypeImpl = storageManager.createLazyValue { computeDefaultType() }
        this.classDescriptorImpl = storageManager.createRecursionTolerantNullableLazyValue({ computeClassDescriptor() }, null)
    }

    private fun computeClassDescriptor(): ClassDescriptor? {
        if (underlyingType.isError) return null
        val underlyingTypeDescriptor = underlyingType.constructor.declarationDescriptor
        return when (underlyingTypeDescriptor) {
            is ClassDescriptor -> underlyingTypeDescriptor
            is TypeAliasDescriptor -> underlyingTypeDescriptor.classDescriptor
            else -> null
        }
    }

    private val lazyTypeConstructorParameters =
            storageManager.createRecursionTolerantLazyValue({ this.computeConstructorTypeParameters() }, emptyList())

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
