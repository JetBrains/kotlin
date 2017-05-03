/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name


internal class VariableManager(val codegen: CodeGenerator) {
    internal interface Record {
        fun load() : LLVMValueRef
        fun store(value: LLVMValueRef)
        fun address() : LLVMValueRef
    }

    inner class SlotRecord(val address: LLVMValueRef, val refSlot: Boolean, val isVar: Boolean) : Record {
        override fun load() : LLVMValueRef = codegen.loadSlot(address, isVar)
        override fun store(value: LLVMValueRef) = codegen.storeAnyLocal(value, address)
        override fun address() : LLVMValueRef = this.address
        override fun toString() = (if (refSlot) "refslot" else "slot") + " for ${address}"
    }

    class ValueRecord(val value: LLVMValueRef, val name: Name) : Record {
        override fun load() : LLVMValueRef = value
        override fun store(value: LLVMValueRef) = throw Error("writing to immutable: ${name}")
        override fun address() : LLVMValueRef = throw Error("no address for: ${name}")
        override fun toString() = "value of ${value} from ${name}"
    }

    val variables: ArrayList<Record> = arrayListOf()
    val contextVariablesToIndex: HashMap<ValueDescriptor, Int> = hashMapOf()

    // Clears inner state of variable manager.
    fun clear() {
        variables.clear()
        contextVariablesToIndex.clear()
    }

    fun createVariable(descriptor: VariableDescriptor, value: LLVMValueRef? = null) : Int {
        // Note that we always create slot for object references for memory management.
        if (!descriptor.isVar && value != null)
            return createImmutable(descriptor, value)
        else
            // Unfortunately, we have to create mutable slots here,
            // as even vals can be assigned on multiple paths. However, we use varness
            // knowledge, as anonymous slots are created only for true vars (for vals
            // their single assigner already have slot).
            return createMutable(descriptor, descriptor.isVar, value)
    }

    fun createMutable(descriptor: VariableDescriptor,
                      isVar: Boolean, value: LLVMValueRef? = null) : Int {
        assert(!contextVariablesToIndex.contains(descriptor))
        val index = variables.size
        val type = codegen.getLLVMType(descriptor.type)
        val slot = codegen.alloca(type, descriptor.name.asString())
        if (value != null)
            codegen.storeAnyLocal(value, slot)
        variables.add(SlotRecord(slot, codegen.isObjectType(type), isVar))
        contextVariablesToIndex[descriptor] = index
        return index
    }

    // Creates anonymous mutable variable.
    // Think of slot reuse.
    fun createAnonymousSlot(value: LLVMValueRef? = null) : LLVMValueRef {
        val index = createAnonymousMutable(codegen.kObjHeaderPtr, value)
        return addressOf(index)
    }

    private fun createAnonymousMutable(type: LLVMTypeRef, value: LLVMValueRef? = null) : Int {
        val index = variables.size
        val slot = codegen.alloca(type)
        if (value != null)
            codegen.storeAnyLocal(value, slot)
        variables.add(SlotRecord(slot, codegen.isObjectType(type), true))
        return index
    }

    fun createImmutable(descriptor: ValueDescriptor, value: LLVMValueRef) : Int {
        if (contextVariablesToIndex.containsKey(descriptor))
            throw Error("$descriptor is already defined")
        val index = variables.size
        variables.add(ValueRecord(value, descriptor.name))
        contextVariablesToIndex[descriptor] = index
        return index
    }

    fun indexOf(descriptor: ValueDescriptor) : Int {
        return contextVariablesToIndex.getOrElse(descriptor) { -1 }
    }

    fun addressOf(index: Int): LLVMValueRef {
        return variables[index].address()
    }

    fun load(index: Int): LLVMValueRef {
        return variables[index].load()
    }

    fun store(value: LLVMValueRef, index: Int) {
        variables[index].store(value)
    }

    fun debugInfoLocalVariableLocation(functionScope: DIScopeOpaqueRef, diType: DITypeOpaqueRef, name:Name, variable: LLVMValueRef, file: DIFileRef, line: Int, location: DILocationRef?) {
        val variableDeclaration = DICreateAutoVariable(
                builder = codegen.context.debugInfo.builder,
                scope = functionScope,
                name = name.asString(),
                file = file,
                line = line,
                type = diType)
        DIInsertDeclarationWithEmptyExpression(
                builder = codegen.context.debugInfo.builder,
                value = variable,
                localVariable = variableDeclaration,
                location = location,
                bb = LLVMGetInsertBlock(codegen.builder))
    }
}
