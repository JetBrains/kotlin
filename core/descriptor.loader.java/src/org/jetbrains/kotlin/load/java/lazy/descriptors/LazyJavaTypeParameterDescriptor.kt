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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class LazyJavaTypeParameterDescriptor(
        private val c: LazyJavaResolverContext,
        public val javaTypeParameter: JavaTypeParameter,
        index: Int,
        containingDeclaration: DeclarationDescriptor
) : AbstractLazyTypeParameterDescriptor(
        c.storageManager,
        containingDeclaration,
        javaTypeParameter.name,
        Variance.INVARIANT,
        /* isReified = */ false,
        index,
        SourceElement.NO_SOURCE
) {

    override fun resolveUpperBounds(): List<KotlinType> {
        val bounds = javaTypeParameter.upperBounds
        if (bounds.isEmpty()) {
            return listOf(LazyJavaTypeResolver.FlexibleJavaClassifierTypeCapabilities.create(
                    c.module.builtIns.anyType,
                    c.module.builtIns.nullableAnyType
            ))
        }
        return bounds.map {
            c.typeResolver.transformJavaType(it, TypeUsage.UPPER_BOUND.toAttributes(upperBoundForTypeParameter = this))
        }
    }
}
