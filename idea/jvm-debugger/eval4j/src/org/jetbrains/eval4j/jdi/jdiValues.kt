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

package org.jetbrains.eval4j.jdi

import com.sun.jdi.ClassObjectReference
import com.sun.jdi.VirtualMachine
import org.jetbrains.eval4j.*
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_STATIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import com.sun.jdi.BooleanValue as jdi_BooleanValue
import com.sun.jdi.ByteValue as jdi_ByteValue
import com.sun.jdi.CharValue as jdi_CharValue
import com.sun.jdi.DoubleValue as jdi_DoubleValue
import com.sun.jdi.FloatValue as jdi_FloatValue
import com.sun.jdi.IntegerValue as jdi_IntegerValue
import com.sun.jdi.LongValue as jdi_LongValue
import com.sun.jdi.ObjectReference as jdi_ObjectReference
import com.sun.jdi.ShortValue as jdi_ShortValue
import com.sun.jdi.Type as jdi_Type
import com.sun.jdi.Value as jdi_Value
import com.sun.jdi.VoidValue as jdi_VoidValue

fun makeInitialFrame(methodNode: MethodNode, arguments: List<Value>): Frame<Value> {
    val isStatic = (methodNode.access and ACC_STATIC) != 0

    val params = Type.getArgumentTypes(methodNode.desc)
    assert(arguments.size == (if (isStatic) params.size else params.size + 1)) {
        "Wrong number of arguments for $methodNode: $arguments"
    }

    val frame = Frame<Value>(methodNode.maxLocals, methodNode.maxStack)
    frame.setReturn(makeNotInitializedValue(Type.getReturnType(methodNode.desc)))

    var index = 0
    for (arg in arguments) {
        frame.setLocal(index++, arg)
        if (arg.size == 2) {
            frame.setLocal(index++, NOT_A_VALUE)
        }
    }

    while (index < methodNode.maxLocals) {
        frame.setLocal(index++, NOT_A_VALUE)
    }

    return frame
}

class JDIFailureException(message: String?, cause: Throwable? = null): RuntimeException(message, cause)

fun jdi_ObjectReference?.asValue(): ObjectValue {
    return when (this) {
        null -> NULL_VALUE
        else -> ObjectValue(this, type().asType())
    }
}

fun jdi_Value?.asValue(): Value {
    return when (this) {
        null -> NULL_VALUE
        is jdi_VoidValue -> VOID_VALUE
        is jdi_BooleanValue -> IntValue(intValue(), Type.BOOLEAN_TYPE)
        is jdi_ByteValue -> IntValue(intValue(), Type.BYTE_TYPE)
        is jdi_ShortValue -> IntValue(intValue(), Type.SHORT_TYPE)
        is jdi_CharValue -> IntValue(intValue(), Type.CHAR_TYPE)
        is jdi_IntegerValue -> IntValue(intValue(), Type.INT_TYPE)
        is jdi_LongValue -> LongValue(longValue())
        is jdi_FloatValue -> FloatValue(floatValue())
        is jdi_DoubleValue -> DoubleValue(doubleValue())
        is jdi_ObjectReference -> this.asValue()
        else -> throw JDIFailureException("Unknown value: $this")
    }
}

fun jdi_Type.asType(): Type = Type.getType(this.signature())

val Value.jdiObj: jdi_ObjectReference?
    get() = this.obj() as jdi_ObjectReference?

val Value.jdiClass: ClassObjectReference?
    get() = this.jdiObj as ClassObjectReference?

fun Value.asJdiValue(vm: VirtualMachine, expectedType: Type): jdi_Value? {
    return when (this) {
        NULL_VALUE -> null
        VOID_VALUE -> vm.mirrorOfVoid()
        is IntValue -> when (expectedType) {
            Type.BOOLEAN_TYPE -> vm.mirrorOf(boolean)
            Type.BYTE_TYPE -> vm.mirrorOf(int.toByte())
            Type.SHORT_TYPE -> vm.mirrorOf(int.toShort())
            Type.CHAR_TYPE -> vm.mirrorOf(int.toChar())
            Type.INT_TYPE -> vm.mirrorOf(int)
            else -> throw JDIFailureException("Unknown value type: $this")
        }
        is LongValue -> vm.mirrorOf(value)
        is FloatValue -> vm.mirrorOf(value)
        is DoubleValue -> vm.mirrorOf(value)
        is ObjectValue -> value as jdi_ObjectReference
        is NewObjectValue -> this.obj() as jdi_ObjectReference
        else -> throw JDIFailureException("Unknown value: $this")
    }
}