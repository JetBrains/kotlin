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
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType

public abstract class ErrorValue : CompileTimeConstant<Unit>(Unit, CompileTimeConstant.Parameters.Impl(true, false, false)) {

    deprecated("Should not be called, for this is not a real value, but a indication of an error")
    override val value: Unit
        get() = throw UnsupportedOperationException()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitErrorValue(this, data)

    public class ErrorValueWithMessage(public val message: String) : ErrorValue() {

        override val type = ErrorUtils.createErrorType(message)

        override fun toString() = message
    }

    companion object {
        public fun create(message: String): ErrorValue {
            return ErrorValueWithMessage(message)
        }
    }
}
