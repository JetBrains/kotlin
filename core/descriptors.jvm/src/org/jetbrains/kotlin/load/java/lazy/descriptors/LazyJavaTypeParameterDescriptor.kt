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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.lazy.LazyJavaAnnotations
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.Variance

class LazyJavaTypeParameterDescriptor(
        private val c: LazyJavaResolverContext,
        val javaTypeParameter: JavaTypeParameter,
        index: Int,
        containingDeclaration: DeclarationDescriptor
) : AbstractLazyTypeParameterDescriptor(
        c.storageManager,
        containingDeclaration,
        javaTypeParameter.name,
        Variance.INVARIANT,
        /* isReified = */ false,
        index,
        SourceElement.NO_SOURCE, c.components.supertypeLoopChecker
) {
    override val annotations = LazyJavaAnnotations(c, javaTypeParameter)

    override fun resolveUpperBounds(): List<KotlinType> {
        val bounds = javaTypeParameter.upperBounds
        if (bounds.isEmpty()) {
            return listOf(KotlinTypeFactory.flexibleType(
                    c.module.builtIns.anyType,
                    c.module.builtIns.nullableAnyType
            ))
        }
        return bounds.map {
            c.typeResolver.transformJavaType(it, TypeUsage.COMMON.toAttributes(upperBoundForTypeParameter = this))
        }
    }

    override fun reportSupertypeLoopError(type: KotlinType) {
        // Do nothing
    }
}
