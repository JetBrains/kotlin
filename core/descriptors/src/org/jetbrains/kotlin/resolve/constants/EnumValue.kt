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

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.classObjectType
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.sure

public class EnumValue(value: ClassDescriptor, usesVariableAsConstant: Boolean) : CompileTimeConstant<ClassDescriptor>(value, true, false, usesVariableAsConstant) {

    override fun getType(kotlinBuiltIns: KotlinBuiltIns): JetType {
        return getType()
    }

    private fun getType(): JetType {
        val type = value.classObjectType
        return type.sure { "Enum entry must have a class object type: " + value }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R {
        return visitor.visitEnumValue(this, data)
    }

    override fun toString(): String {
        return "${getType()}.${value.getName()}"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        return value == (o as? EnumValue)?.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

