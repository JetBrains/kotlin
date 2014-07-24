/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.descriptors.SourceElement

class LazyJavaTypeParameterDescriptor(
        private val c: LazyJavaResolverContextWithTypes,
        public val javaTypeParameter: JavaTypeParameter,
        containingDeclaration: DeclarationDescriptor
) : AbstractLazyTypeParameterDescriptor(
        c.storageManager,
        containingDeclaration,
        javaTypeParameter.getName(),
        Variance.INVARIANT,
        /* isReified = */ false,
        javaTypeParameter.getIndex(),
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
