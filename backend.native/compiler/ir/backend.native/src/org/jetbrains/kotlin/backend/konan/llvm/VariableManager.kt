package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.descriptors.*
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

    class ValueRecord(val value: LLVMValueRef, val descriptor: ValueDescriptor) : Record {
        override fun load() : LLVMValueRef = value
        override fun store(value: LLVMValueRef) = throw Error("writing to immutable: ${descriptor}")
        override fun address() : LLVMValueRef = throw Error("no address for: ${descriptor}")
        override fun toString() = "value of ${value} from ${descriptor}"
    }

    val variables: ArrayList<Record> = arrayListOf()
    val descriptors: HashMap<Pair<ValueDescriptor, CodeContext>, Int> = hashMapOf()

    // Clears inner state of variable manager.
    fun clear() {
        variables.clear()
        descriptors.clear()
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
        assert(descriptors.get(scoped) == null)
        val index = variables.size
        val type = codegen.getLLVMType(descriptor.type)
        val slot = codegen.alloca(type, descriptor.name.asString())
        if (value != null)
            codegen.storeAnyLocal(value, slot)
        variables.add(SlotRecord(slot, codegen.isObjectType(type), isVar))
        descriptors[scoped] = index
        return index
    }

    // Creates anonymous mutable variable.
    fun createAnonymousMutable(type: KotlinType, value: LLVMValueRef? = null) : Int {
        return createAnonymousMutable(codegen.getLLVMType(type), value)
    }

    // Think of slot reuse.
    fun createAnonymousSlot(value: LLVMValueRef? = null) : LLVMValueRef {
        val index = createAnonymousMutable(codegen.kObjHeaderPtr, value)
        return addressOf(index)
    }

    fun createAnonymousMutable(type: LLVMTypeRef, value: LLVMValueRef? = null) : Int {
        val index = variables.size
        val slot = codegen.alloca(type)
        if (value != null)
            codegen.storeAnyLocal(value, slot)
        variables.add(SlotRecord(slot, codegen.isObjectType(type), true))
        return index
    }

    fun createImmutable(scoped: Pair<ValueDescriptor, CodeContext>, value: LLVMValueRef) : Int {
        val descriptor = scoped.first
        assert(descriptors.get(scoped) == null)
        val index = variables.size
        variables.add(ValueRecord(value, descriptor))
        descriptors[scoped] = index
        return index
    }

    fun indexOf(scoped: Pair<ValueDescriptor, CodeContext>) : Int {
        return descriptors.getOrElse(scoped) { -1 }
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
