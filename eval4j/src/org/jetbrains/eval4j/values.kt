/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.eval4j

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.LabelNode

public trait Value : org.jetbrains.org.objectweb.asm.tree.analysis.Value {
    public val asmType: Type
    public val valid: Boolean
    override fun getSize(): Int = asmType.getSize()

    override fun toString(): String
}

object NOT_A_VALUE: Value {
    override val asmType = Type.getType("<invalid>")
    override val valid = false
    override fun getSize(): Int = 1

    override fun toString() = "NOT_A_VALUE"
}

object VOID_VALUE: Value {
    override val asmType: Type = Type.VOID_TYPE
    override val valid: Boolean = false
    override fun toString() = "VOID_VALUE"
}

fun makeNotInitializedValue(t: Type): Value? {
    return when (t.getSort()) {
        Type.VOID -> null
        else -> NotInitialized(t)
    }
}

class NotInitialized(override val asmType: Type): Value {
    override val valid = false
    override fun toString() = "NotInitialized: $asmType"
}

abstract class AbstractValueBase<V>(
        override val asmType: Type
) : Value {
    override val valid = true
    public abstract val value: V

    override fun toString() = "$value: $asmType"

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractValue<*>) return false

        return value == other.value && asmType == other.asmType
    }

    override fun hashCode(): Int {
        return value.hashCode() + 17 * asmType.hashCode()
    }
}

abstract class AbstractValue<V>(
        override val value: V,
        asmType: Type
) : AbstractValueBase<V>(asmType)

class IntValue(value: Int, asmType: Type): AbstractValue<Int>(value, asmType)
class LongValue(value: Long): AbstractValue<Long>(value, Type.LONG_TYPE)
class FloatValue(value: Float): AbstractValue<Float>(value, Type.FLOAT_TYPE)
class DoubleValue(value: Double): AbstractValue<Double>(value, Type.DOUBLE_TYPE)
public class ObjectValue(value: Any?, asmType: Type): AbstractValue<Any?>(value, asmType)
class NewObjectValue(asmType: Type): AbstractValueBase<Any?>(asmType) {
    override var value: Any? = null
}

class LabelValue(value: LabelNode): AbstractValue<LabelNode>(value, Type.VOID_TYPE)

fun boolean(v: Boolean) = IntValue(if (v) 1 else 0, Type.BOOLEAN_TYPE)
fun byte(v: Byte) = IntValue(v.toInt(), Type.BYTE_TYPE)
fun short(v: Short) = IntValue(v.toInt(), Type.SHORT_TYPE)
fun char(v: Char) = IntValue(v.toInt(), Type.CHAR_TYPE)
fun int(v: Int) = IntValue(v, Type.INT_TYPE)
fun long(v: Long) = LongValue(v)
fun float(v: Float) = FloatValue(v)
fun double(v: Double) = DoubleValue(v)
//fun obj<T>(v: T, t: Type = if (v != null) Type.getType(v.javaClass) else Type.getType(javaClass<Any>())) = ObjectValue(v, t)

val NULL_VALUE = ObjectValue(null, Type.getObjectType("null"))

val Value.boolean: Boolean get() = (this as IntValue).value == 1
val Value.int: Int get() = (this as IntValue).value
val Value.long: Long get() = (this as LongValue).value
val Value.float: Float get() = (this as FloatValue).value
val Value.double: Double get() = (this as DoubleValue).value
fun Value.obj(expectedType: Type = asmType): Any? {
    if (this is NewObjectValue) {
        val v = value
        if (v == null) throw IllegalStateException("Trying to access an unitialized object: $this")
        return v
    }
    return when {
        expectedType == Type.BOOLEAN_TYPE -> this.boolean
        expectedType == Type.SHORT_TYPE -> (this as IntValue).int.toShort()
        expectedType == Type.BYTE_TYPE -> (this as IntValue).int.toByte()
        expectedType == Type.CHAR_TYPE -> (this as IntValue).int.toChar()
        else -> (this as AbstractValue<*>).value
    }
}

fun <T: Any> T?.checkNull(): T {
    if (this == null) {
        throwEvalException(NullPointerException())
    }
    return this
}

fun throwEvalException(e: Throwable): Nothing {
    throw ThrownFromEvalException(e)
}

fun throwBrokenCodeException(e: Throwable): Nothing {
    throw BrokenCode(e)
}