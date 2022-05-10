/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

/**
 * This class has strict `equals` implementation, that does not accept its subclasses,
 * unlike BasicValue that requires `other` instance only to be a subtype of BasicValue and must have the same `type`
 */
open class StrictBasicValue(type: Type?) : BasicValue(type) {
    companion object {
        @JvmField
        val UNINITIALIZED_VALUE = StrictBasicValue(null)
        @JvmField
        val INT_VALUE = StrictBasicValue(Type.INT_TYPE)
        @JvmField
        val FLOAT_VALUE = StrictBasicValue(Type.FLOAT_TYPE)
        @JvmField
        val LONG_VALUE = StrictBasicValue(Type.LONG_TYPE)
        @JvmField
        val DOUBLE_VALUE = StrictBasicValue(Type.DOUBLE_TYPE)
        @JvmField
        val BOOLEAN_VALUE = StrictBasicValue(Type.BOOLEAN_TYPE)
        @JvmField
        val CHAR_VALUE = StrictBasicValue(Type.CHAR_TYPE)
        @JvmField
        val BYTE_VALUE = StrictBasicValue(Type.BYTE_TYPE)
        @JvmField
        val SHORT_VALUE = StrictBasicValue(Type.SHORT_TYPE)
        @JvmField
        val REFERENCE_VALUE = StrictBasicValue(Type.getObjectType("java/lang/Object"))

        @JvmField
        val NULL_VALUE = StrictBasicValue(Type.getObjectType("java/lang/Object"))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        if (!super.equals(other)) return false

        other as StrictBasicValue

        if (this === NULL_VALUE) return other === NULL_VALUE
        if (other === NULL_VALUE) return this === NULL_VALUE

        if (type != other.type) return false

        return true
    }

    override fun hashCode() = (type?.hashCode() ?: 0)

    override fun toString(): String {
        val inner = when {
            this === REFERENCE_VALUE -> "R"
            this === NULL_VALUE -> "null"
            this === UNINITIALIZED_VALUE -> "."
            else -> super.toString()
        }
        return "${this::class.java.simpleName}($inner)"
    }
}
