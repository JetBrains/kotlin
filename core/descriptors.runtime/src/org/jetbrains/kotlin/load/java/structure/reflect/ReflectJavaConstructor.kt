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

import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

public class ReflectJavaConstructor(override val member: Constructor<*>) : ReflectJavaMember(), JavaConstructor {
    // TODO: test local/anonymous classes
    override fun getValueParameters(): List<JavaValueParameter> {
        val types = member.getGenericParameterTypes()
        if (types.isEmpty()) return emptyList()

        val klass = member.getDeclaringClass()

        val realTypes = when {
            klass.getDeclaringClass() != null && !Modifier.isStatic(klass.getModifiers()) -> types.copyOfRange(1, types.size())
            else -> types
        }

        val annotations = member.getParameterAnnotations()
        val realAnnotations = when {
            annotations.size() < realTypes.size() -> throw IllegalStateException("Illegal generic signature: $member")
            annotations.size() > realTypes.size() -> annotations.copyOfRange(annotations.size() - realTypes.size(), annotations.size())
            else -> annotations
        }

        return getValueParameters(realTypes, realAnnotations, member.isVarArgs())
    }

    override fun getTypeParameters(): List<JavaTypeParameter> {
        // TODO
        return listOf()
    }
}
