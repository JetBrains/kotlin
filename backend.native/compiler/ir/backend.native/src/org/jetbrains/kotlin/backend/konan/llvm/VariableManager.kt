package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*


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

    data class ScopedVariable private constructor(val descriptor: ValueDescriptor, val context: CodeContext)
    {
        constructor(scoped: Pair<ValueDescriptor, CodeContext>) : this(scoped.first, scoped.second)
    }

    val variables: ArrayList<Record> = arrayListOf()
    val contextVariablesToIndex: HashMap<ScopedVariable, Int> = hashMapOf()

    // Clears inner state of variable manager.
    fun clear() {
        variables.clear()
        contextVariablesToIndex.clear()
    }

    fun createVariable(scoped: Pair<VariableDescriptor, CodeContext>, value: LLVMValueRef? = null) : Int {
        // Note that we always create slot for object references for memory management.
        val descriptor = scoped.first
        if (!descriptor.isVar && value != null)
            return createImmutable(scoped, value)
        else
            // Unfortunately, we have to create mutable slots here,
            // as even vals can be assigned on multiple paths. However, we use varness
            // knowledge, as anonymous slots are created only for true vars (for vals
            // their single assigner already have slot).
            return createMutable(scoped, descriptor.isVar, value)
    }

    fun createMutable(scoped: Pair<VariableDescriptor, CodeContext>,
                      isVar: Boolean, value: LLVMValueRef? = null) : Int {
        val descriptor = scoped.first
        val scopedVariable = ScopedVariable(scoped)
        assert(!contextVariablesToIndex.contains(scopedVariable))
        val index = variables.size
        val type = codegen.getLLVMType(descriptor.type)
        val slot = codegen.alloca(type, scopedVariable.descriptor.name.asString())
        if (value != null)
            codegen.storeAnyLocal(value, slot)
        variables.add(SlotRecord(slot, codegen.isObjectType(type), isVar))
        contextVariablesToIndex[scopedVariable] = index
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

    fun createImmutable(scoped: Pair<ValueDescriptor, CodeContext>, value: LLVMValueRef) : Int {
        val scopedVariable = ScopedVariable(scoped)
        if (contextVariablesToIndex.containsKey(scopedVariable))
            throw Error("${scopedVariable.descriptor} is already defined")
        val index = variables.size
        variables.add(ValueRecord(value, scopedVariable.descriptor.name))
        contextVariablesToIndex[scopedVariable] = index
        return index
    }

    fun indexOf(scoped: Pair<ValueDescriptor, CodeContext>) : Int {
        return contextVariablesToIndex.getOrElse(ScopedVariable(scoped)) { -1 }
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
}
