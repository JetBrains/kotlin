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
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.types.KotlinType

class LazyTypeAliasDescriptor(
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,
        name: Name,
        sourceElement: SourceElement,
        visibility: Visibility
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, sourceElement, visibility),
        TypeAliasDescriptor {

    private lateinit var underlyingTypeImpl: NotNullLazyValue<KotlinType>
    private lateinit var expandedTypeImpl: NotNullLazyValue<KotlinType>

    override val underlyingType: KotlinType get() = underlyingTypeImpl()
    override val expandedType: KotlinType get() = expandedTypeImpl()

    fun initialize(
            declaredTypeParameters: List<TypeParameterDescriptor>,
            lazyUnderlyingType: NotNullLazyValue<KotlinType>,
            lazyExpandedType: NotNullLazyValue<KotlinType>
    ) {
        super.initialize(declaredTypeParameters)
        this.underlyingTypeImpl = lazyUnderlyingType
        this.expandedTypeImpl = lazyExpandedType
    }

    companion object {
        @JvmStatic fun create(
                containingDeclaration: DeclarationDescriptor,
                annotations: Annotations,
                name: Name,
                sourceElement: SourceElement,
                visibility: Visibility
        ): LazyTypeAliasDescriptor =
                LazyTypeAliasDescriptor(containingDeclaration, annotations, name, sourceElement, visibility)
    }
}
