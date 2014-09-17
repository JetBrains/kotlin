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

package org.jetbrains.jet.codegen

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.jet.codegen.StackValue.StackValueWithReceiver
import org.jetbrains.jet.codegen.StackValue.StackValueWithoutReceiver
import org.jetbrains.jet.codegen.StackValue.StackValueWithSimpleReceiver

public fun castValue(value: StackValue, castType: Type): StackValue {
    return if (value is StackValueWithReceiver) CastValueWithReceiver(value, castType) else CastValue(value, castType)
}

class CastValueWithReceiver(val value: StackValueWithReceiver, val castType: Type) : StackValueWithSimpleReceiver(castType, !value.hasReceiver(true), !value.hasReceiver(false), value.receiver), StackValueI by value {

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        value.putReceiver(v, isRead )
    }

    override fun putNoReceiver(type: Type, v: InstructionAdapter) {
        value.putNoReceiver(type, v)
    }

    override fun hasReceiver(isRead: Boolean): Boolean {
        return value.hasReceiver(isRead)
    }
}

class CastValue(val value: StackValue, val castType: Type) : StackValueWithoutReceiver(castType), StackValueI by value {

}

class FunctionCallStackValue(val resultType: Type, val lambda: (v: InstructionAdapter)-> Unit) : StackValueWithoutReceiver(resultType) {

    override fun put(type: Type, v: InstructionAdapter) {
        lambda(v)
        coerceTo(type, v)
    }

    override fun store(topOfStackType: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException();
    }
}

public class StackValueWithLeaveTask(val stackValue: StackValue, val leaveTasks: StackValueWithLeaveTask.()-> Unit) : StackValueWithReceiver(stackValue.type, if (stackValue is StackValueWithReceiver) stackValue.receiver else StackValue.none()), StackValueI by stackValue {

    override fun put(type: Type, v: InstructionAdapter) {
        stackValue.put(type, v)
        leaveTasks()
    }

    override fun condJump(label: Label, jumpIfFalse: Boolean, v: InstructionAdapter ) {
        stackValue.condJump(label, jumpIfFalse, v)
        leaveTasks()
    }

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        if (stackValue is StackValueWithReceiver) {
            stackValue.putReceiver(v, isRead)
        }
    }

    override fun putNoReceiver(type: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException()
    }

    override fun hasReceiver(isRead: Boolean): Boolean {
        throw UnsupportedOperationException()
    }

    override fun store(topOfStackType: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException();
    }
}

class OperationStackValue(val resultType: Type, val lambda: (v: InstructionAdapter)-> Unit) : StackValueWithoutReceiver(resultType) {

    override fun put(type: Type, v: InstructionAdapter) {
        lambda(v)
        coerceTo(type, v)
    }

    override fun store(topOfStackType: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException();
    }
}