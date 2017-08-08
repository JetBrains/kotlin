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

package org.jetbrains.kotlin.codegen

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class CoercionValue(
        val value: StackValue,
        private val castType: Type
) : StackValue(castType, value.canHaveSideEffects()) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        value.putSelector(value.type, v)
        StackValue.coerce(value.type, castType, v)
        StackValue.coerce(castType, type, v)
    }

    override fun storeSelector(topOfStackType: Type, v: InstructionAdapter) {
        value.storeSelector(topOfStackType, v)
    }

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        value.putReceiver(v, isRead)
    }

    override fun isNonStaticAccess(isRead: Boolean): Boolean {
        return value.isNonStaticAccess(isRead)
    }
}


class StackValueWithLeaveTask(
        val stackValue: StackValue,
        val leaveTasks: (StackValue) -> Unit
) : StackValue(stackValue.type) {

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        stackValue.putReceiver(v, isRead)
    }

    override fun putSelector(type: Type, v: InstructionAdapter) {
        stackValue.putSelector(type, v)
        leaveTasks(stackValue)
    }
}

open class OperationStackValue(resultType: Type, val lambda: (v: InstructionAdapter) -> Unit) : StackValue(resultType) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        lambda(v)
        coerceTo(type, v)
    }
}

class FunctionCallStackValue(resultType: Type, lambda: (v: InstructionAdapter) -> Unit) : OperationStackValue(resultType, lambda)
