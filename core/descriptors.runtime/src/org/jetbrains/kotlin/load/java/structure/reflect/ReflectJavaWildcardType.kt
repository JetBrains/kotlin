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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaTypeProvider
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType
import java.lang.reflect.WildcardType

public class ReflectJavaWildcardType(override val type: WildcardType): ReflectJavaType(), JavaWildcardType {
    override fun getBound(): ReflectJavaType? {
        val upperBounds = type.getUpperBounds()
        val lowerBounds = type.getLowerBounds()
        if (upperBounds.size() > 1 || lowerBounds.size() > 1) {
            throw UnsupportedOperationException("Wildcard types with many bounds are not yet supported: $type")
        }
        return when {
            lowerBounds.size() == 1 -> ReflectJavaType.create(lowerBounds.single())
            upperBounds.size() == 1 -> upperBounds.single().let { ub -> if (ub != javaClass<Any>()) ReflectJavaType.create(ub) else null }
            else -> null
        }
    }

    override fun isExtends() = type.getUpperBounds().firstOrNull() != javaClass<Any>()

    override fun getTypeProvider(): JavaTypeProvider = throw UnsupportedOperationException()
}
