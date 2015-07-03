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

public class CharValue(
        value: Char,
        parameters: CompileTimeConstant.Parameters
) : IntegerValueConstant<Char>(value, parameters) {

    override fun getType(kotlinBuiltIns: KotlinBuiltIns) = kotlinBuiltIns.getCharType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitCharValue(this, data)

    override fun toString() = "\\u%04X ('%s')".format(value.toInt(), getPrintablePart(value))

    private fun getPrintablePart(c: Char): String {
        when (c) {
            '\b' -> return "\\b"
            '\t' -> return "\\t"
            '\n' -> return "\\n"
            //TODO_R: can't escape form feed in Kotlin
            12.toChar() -> return "\\f"
            '\r' -> return "\\r"
            else -> return if (isPrintableUnicode(c)) Character.toString(c) else "?"
        }
    }

    private fun isPrintableUnicode(c: Char): Boolean {
        val t = Character.getType(c).toByte()
        return t != Character.UNASSIGNED &&
               t != Character.LINE_SEPARATOR &&
               t != Character.PARAGRAPH_SEPARATOR &&
               t != Character.CONTROL &&
               t != Character.FORMAT &&
               t != Character.PRIVATE_USE &&
               t != Character.SURROGATE
    }
}
