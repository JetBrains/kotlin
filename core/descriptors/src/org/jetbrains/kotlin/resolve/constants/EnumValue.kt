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

public class EnumValue(
        value: ClassDescriptor,
        usesVariableAsConstant: Boolean
) : CompileTimeConstant<ClassDescriptor>(value, true, false, usesVariableAsConstant) {

    override fun getType(kotlinBuiltIns: KotlinBuiltIns) = getType()

    private fun getType() = value.classObjectType.sure { "Enum entry must have a class object type: " + value }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitEnumValue(this, data)

    override fun toString() = "${getType()}.${value.getName()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return value == (other as EnumValue).value
    }

    override fun hashCode() = value.hashCode()
}

