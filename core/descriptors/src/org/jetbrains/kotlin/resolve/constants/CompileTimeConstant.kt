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

public abstract class CompileTimeConstant<T> protected constructor(
        public open val value: T,
        public val parameters: CompileTimeConstant.Parameters
) {
    public open fun canBeUsedInAnnotations(): Boolean = parameters.canBeUsedInAnnotation

    public open fun isPure(): Boolean = parameters.isPure

    public open fun usesVariableAsConstant(): Boolean = parameters.usesVariableAsConstant

    public abstract val type: JetType

    public abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun toString() = value.toString()

    public interface Parameters {
        public open val canBeUsedInAnnotation: Boolean
        public open val isPure: Boolean
        public open val usesVariableAsConstant: Boolean

        public class Impl(
                override val canBeUsedInAnnotation: Boolean,
                override val isPure: Boolean,
                override val usesVariableAsConstant: Boolean
        ) : Parameters

        public object ThrowException : Parameters {
            override val canBeUsedInAnnotation: Boolean
                get() = error("Should not be called")
            override val isPure: Boolean
                get() = error("Should not be called")
            override val usesVariableAsConstant: Boolean
                get() = error("Should not be called")
        }
    }
}