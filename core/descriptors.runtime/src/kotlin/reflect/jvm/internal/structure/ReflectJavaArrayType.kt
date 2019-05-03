/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal.structure

import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type

class ReflectJavaArrayType(override val reflectType: Type) : ReflectJavaType(), JavaArrayType {
    override val componentType: ReflectJavaType = with(reflectType) {
        when {
            this is GenericArrayType -> create(genericComponentType)
            this is Class<*> && isArray() -> create(getComponentType())
            else -> throw IllegalArgumentException("Not an array type (${reflectType::class.java}): $reflectType")
        }
    }
}
