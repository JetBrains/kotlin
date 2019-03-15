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

import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ReflectJavaValueParameter(
    override val type: ReflectJavaType,
    private val reflectAnnotations: Array<Annotation>,
    private val reflectName: String?,
    override val isVararg: Boolean
) : ReflectJavaElement(), JavaValueParameter {
    override val annotations: List<ReflectJavaAnnotation>
        get() = reflectAnnotations.getAnnotations()

    override fun findAnnotation(fqName: FqName) =
        reflectAnnotations.findAnnotation(fqName)

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override val name: Name?
        get() = reflectName?.let(Name::guessByFirstCharacter)

    override fun toString() = this::class.java.name + ": " + (if (isVararg) "vararg " else "") + name + ": " + type
}
