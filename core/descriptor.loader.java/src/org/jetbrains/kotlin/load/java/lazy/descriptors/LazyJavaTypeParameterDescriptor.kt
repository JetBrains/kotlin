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

import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.descriptors.SourceElement

class LazyJavaTypeParameterDescriptor(
        private val c: LazyJavaResolverContext,
        public val javaTypeParameter: JavaTypeParameter,
        index: Int,
        containingDeclaration: DeclarationDescriptor
) : AbstractLazyTypeParameterDescriptor(
        c.storageManager,
        containingDeclaration,
        javaTypeParameter.getName(),
        Variance.INVARIANT,
        /* isReified = */ false,
        index,
        SourceElement.NO_SOURCE
) {

    override fun resolveUpperBounds(): Set<JetType> {
        val bounds = javaTypeParameter.getUpperBounds()
        if (bounds.isEmpty()) {
            return setOf(KotlinBuiltIns.getInstance().getDefaultBound())
        }
        else {
            return bounds.map {
                javaType -> c.typeResolver.transformJavaType(javaType, TypeUsage.UPPER_BOUND.toAttributes())
            }.toSet()
        }
    }

}
