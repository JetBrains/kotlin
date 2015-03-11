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

import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import java.lang.reflect.Method

public class ReflectJavaMethod(override val member: Method) : ReflectJavaMember(), JavaMethod {
    override fun getValueParameters(): List<JavaValueParameter> =
            getValueParameters(member.getGenericParameterTypes(), member.getParameterAnnotations(), member.isVarArgs())

    override fun getReturnType() = ReflectJavaType.create(member.getGenericReturnType())

    override fun hasAnnotationParameterDefaultValue() = member.getDefaultValue() != null

    override fun getTypeParameters() = member.getTypeParameters().map { ReflectJavaTypeParameter(it) }
}
