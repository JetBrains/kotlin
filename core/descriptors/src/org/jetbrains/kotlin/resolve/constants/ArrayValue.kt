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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.types.JetType

public class ArrayValue(value: List<CompileTimeConstant<*>>, private val type: JetType, canBeUsedInAnnotations: Boolean, usesVariableAsConstant: Boolean) : CompileTimeConstant<List<CompileTimeConstant<*>>>(value, canBeUsedInAnnotations, false, usesVariableAsConstant) {

    init {
        assert(KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) { "Type should be an array, but was " + type + ": " + value }
    }

    override fun getType(kotlinBuiltIns: KotlinBuiltIns): JetType {
        return type
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R {
        return visitor.visitArrayValue(this, data)
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as? ArrayValue ?: return false

        if (value == null) {
            return that.value == null
        }

        var i = 0
        for (thisObject in value) {
            if (thisObject != that.value.get(i)) {
                return false
            }
            i++
        }

        return true
    }

    override fun hashCode(): Int {
        var hashCode = 0
        if (value == null) return hashCode
        for (o in value) {
            hashCode += o.hashCode()
        }
        return hashCode
    }
}

