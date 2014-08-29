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

import java.lang.reflect.Field
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.FqName

public class ReflectJavaField(field: Field) : ReflectJavaMember(field), JavaField {
    val field: Field
        get() = member as Field

    override fun getAnnotations(): Collection<JavaAnnotation> {
        // TODO
        return listOf()
    }

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        // TODO
        return null
    }

    override fun isEnumEntry() = field.isEnumConstant()

    override fun getType() = ReflectJavaType.create(field.getGenericType()!!)
}
