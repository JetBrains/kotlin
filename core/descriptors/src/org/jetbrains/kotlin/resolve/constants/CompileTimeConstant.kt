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

public abstract class CompileTimeConstant<T> protected constructor(public open val value: T, canBeUsedInAnnotations: Boolean, isPure: Boolean, usesVariableAsConstant: Boolean) {
    private val flags: Int

    init {
        flags = (if (isPure) IS_PURE_MASK else 0) or (if (canBeUsedInAnnotations) CAN_BE_USED_IN_ANNOTATIONS_MASK else 0) or (if (usesVariableAsConstant) USES_VARIABLE_AS_CONSTANT_MASK else 0)
    }

    public fun canBeUsedInAnnotations(): Boolean {
        return (flags and CAN_BE_USED_IN_ANNOTATIONS_MASK) != 0
    }

    public fun isPure(): Boolean {
        return (flags and IS_PURE_MASK) != 0
    }

    public fun usesVariableAsConstant(): Boolean {
        return (flags and USES_VARIABLE_AS_CONSTANT_MASK) != 0
    }

    public abstract fun getType(kotlinBuiltIns: KotlinBuiltIns): JetType

    public abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    companion object {

        /*
    * if is pure is false then constant type cannot be changed
    * ex1. val a: Long = 1.toInt() (TYPE_MISMATCH error, 1.toInt() isn't pure)
    * ex2. val b: Int = a (TYPE_MISMATCH error, a isn't pure)
    *
    */
        private val IS_PURE_MASK = 1
        private val CAN_BE_USED_IN_ANNOTATIONS_MASK = 1 shl 1
        private val USES_VARIABLE_AS_CONSTANT_MASK = 1 shl 2
    }
}
