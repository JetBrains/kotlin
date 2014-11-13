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
import org.jetbrains.jet.codegen.StackValue.StackValueWithSimpleReceiver

class CoercionValue(
        val value: StackValue,
        val castType: Type
) : StackValue(castType) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        value.putSelector(castType, v)
        StackValue.coerce(castType, type, v)
    }

    override fun storeSelector(topOfStackType: Type, v: InstructionAdapter) {
        value.storeSelector(topOfStackType, v)
    }

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        value.putReceiver(v, isRead)
    }

    override fun condJump(label: Label, jumpIfFalse: Boolean, v: InstructionAdapter) {
        value.condJump(label, jumpIfFalse, v)
    }

    override fun hasReceiver(isRead: Boolean): Boolean {
        return value.hasReceiver(isRead)
    }
}


public class StackValueWithLeaveTask(
        val stackValue: StackValue,
        val leaveTasks: StackValueWithLeaveTask.() -> Unit
) : StackValue(stackValue.type) {

    override fun put(type: Type, v: InstructionAdapter, skipReceiver: Boolean) {
        stackValue.put(type, v, skipReceiver)
        leaveTasks()
    }

    override fun condJump(label: Label, jumpIfFalse: Boolean, v: InstructionAdapter) {
        stackValue.condJump(label, jumpIfFalse, v)
        leaveTasks()
    }

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        stackValue.putReceiver(v, isRead)
    }

    override fun putSelector(type: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException()
    }


    override fun storeSelector(topOfStackType: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException();
    }
}

open class OperationStackValue(val resultType: Type, val lambda: (v: InstructionAdapter)-> Unit) : StackValue(resultType) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        lambda(v)
        coerceTo(type, v)
    }
}

class FunctionCallStackValue(resultType: Type, lambda: (v: InstructionAdapter)-> Unit) : OperationStackValue(resultType, lambda)