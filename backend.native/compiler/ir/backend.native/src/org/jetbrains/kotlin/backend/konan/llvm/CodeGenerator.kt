package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType

internal class CodeGenerator(override val context: Context) : ContextUtils {
    var function: LLVMValueRef? = null
    var returnType: LLVMTypeRef? = null
    val returns: MutableMap<LLVMBasicBlockRef, LLVMValueRef> = mutableMapOf()
    // TODO: remove, to make CodeGenerator descriptor-agnostic.
    var constructedClass: ClassDescriptor? = null
    val vars = VariableManager(this)
    var returnSlot: LLVMValueRef? = null
    var slotsPhi: LLVMValueRef? = null
    var slotCount = 0

    fun prologue(descriptor: FunctionDescriptor) {
        prologue(llvmFunction(descriptor),
                LLVMGetReturnType(getLlvmFunctionType(descriptor))!!)
        if (descriptor is ConstructorDescriptor) {
            constructedClass = descriptor.constructedClass
        }
    }

    fun prologue(function:LLVMValueRef, returnType:LLVMTypeRef) {
        assert(returns.size == 0)
        assert(this.function != function)

        if (isObjectType(returnType)) {
            this.returnSlot = LLVMGetParam(function, numParameters(function.type) - 1)
        }
        this.function = function
        this.returnType = returnType
        this.constructedClass = null
        prologueBb = LLVMAppendBasicBlock(function, "prologue")
        entryBb = LLVMAppendBasicBlock(function, "entry")
        epilogueBb = LLVMAppendBasicBlock(function, "epilogue")
        cleanupLandingpad = LLVMAppendBasicBlock(function, "cleanup_landingpad")!!
        positionAtEnd(entryBb!!)
        slotsPhi = phi(kObjHeaderPtrPtr)
        slotCount = 0
    }

    fun epilogue() {
        appendingTo(prologueBb!!) {
            val slots = if (slotCount > 0)
                LLVMBuildArrayAlloca(builder, kObjHeaderPtr, Int32(slotCount).llvm, "")!!
            else
                kNullObjHeaderPtrPtr
            if (slotCount > 0) {
                // Zero-init slots.
                val slotsMem = bitcast(kInt8Ptr, slots)
                val pointerSize = LLVMABISizeOfType(llvmTargetData, kObjHeaderPtr).toInt()
                val alignment = LLVMABIAlignmentOfType(llvmTargetData, kObjHeaderPtr)
                call(context.llvm.memsetFunction,
                        listOf(slotsMem, Int8(0).llvm,
                                Int32(slotCount * pointerSize).llvm, Int32(alignment).llvm,
                                Int1(0).llvm))
            }
            addPhiIncoming(slotsPhi!!, prologueBb!! to slots)
            br(entryBb!!)
        }

        appendingTo(epilogueBb!!) {
            when {
               returnType == voidType -> {
                   releaseVars()
                   assert(returnSlot == null)
                   LLVMBuildRetVoid(builder)
               }
               returns.size > 0 -> {
                    val returnPhi = phi(returnType!!)
                    addPhiIncoming(returnPhi, *returns.toList().toTypedArray())
                    if (returnSlot != null) {
                        updateLocalRef(returnPhi, returnSlot!!)
                    }
                    releaseVars()
                    LLVMBuildRet(builder, returnPhi)
               }
               // Do nothing, all paths throw.
               else -> LLVMBuildUnreachable(builder)
            }
        }

        appendingTo(cleanupLandingpad!!) {
            val landingpad = gxxLandingpad(numClauses = 0)
            LLVMSetCleanup(landingpad, 1)
            releaseVars()
            LLVMBuildResume(builder, landingpad)
        }

        returns.clear()
        vars.clear()
        returnSlot = null
        slotsPhi = null
    }

    fun releaseVars() {
        if (slotCount > 0) {
            call(context.llvm.releaseLocalRefsFunction,
                    listOf(slotsPhi!!, Int32(slotCount).llvm))
        }
    }

    private var prologueBb: LLVMBasicBlockRef? = null
    private var entryBb: LLVMBasicBlockRef? = null
    private var epilogueBb: LLVMBasicBlockRef? = null
    private var cleanupLandingpad: LLVMBasicBlockRef? = null

    fun setName(value: LLVMValueRef, name: String) = LLVMSetValueName(value, name)
    fun getName(value: LLVMValueRef) = LLVMGetValueName(value)?.asCString().toString()

    fun plus  (arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildAdd (builder, arg0, arg1, name)!!
    fun mul   (arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildMul (builder, arg0, arg1, name)!!
    fun minus (arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildSub (builder, arg0, arg1, name)!!
    fun div   (arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildSDiv(builder, arg0, arg1, name)!!
    fun srem  (arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildSRem(builder, arg0, arg1, name)!!

    /* integers comparisons */
    fun icmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ,  arg0, arg1, name)!!
    fun icmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, name)!!
    fun icmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, name)!!
    fun icmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, name)!!
    fun icmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, name)!!
    fun icmpNe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntNE,  arg0, arg1, name)!!

    fun ucmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntUGT, arg0, arg1, name)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, name)!!

    fun bitcast(type: LLVMTypeRef?, value: LLVMValueRef, name: String = "") = LLVMBuildBitCast(builder, value, type, name)!!

    fun intToPtr(imm: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildIntToPtr(builder, imm, DestTy, Name)!!

    fun alloca(type: LLVMTypeRef?, name: String = ""): LLVMValueRef {
        if (isObjectType(type!!)) {
            return gep(slotsPhi!!, Int32(slotCount++).llvm)
        }
        appendingTo(prologueBb!!) {
            return LLVMBuildAlloca(builder, type, name)!!
        }
    }
    fun load(value: LLVMValueRef, name: String = ""): LLVMValueRef {
        val result = LLVMBuildLoad(builder, value, name)!!
        // Use loadSlot() API for that.
        assert(!isObjectRef(value))
        return result
    }
    fun loadSlot(address: LLVMValueRef, isVar: Boolean, name: String = "") : LLVMValueRef {
        val value = LLVMBuildLoad(builder, address, name)!!
        if (isObjectRef(value) && isVar) {
            val slot = alloca(LLVMTypeOf(value))
            storeAnyLocal(value, slot)
        }
        return value
    }
    fun store(value: LLVMValueRef, ptr: LLVMValueRef) {
        // Use updateRef() or storeAny() API for that.
        assert(!isObjectRef(value))
        LLVMBuildStore(builder, value, ptr)
    }
    fun storeAnyLocal(value: LLVMValueRef, ptr: LLVMValueRef) {
        if (isObjectRef(value)) {
            updateLocalRef(value, ptr)
        } else {
            LLVMBuildStore(builder, value, ptr)
        }
    }
    fun storeAnyGlobal(value: LLVMValueRef, ptr: LLVMValueRef) {
        if (isObjectRef(value)) {
            updateGlobalRef(value, ptr)
        } else {
            LLVMBuildStore(builder, value, ptr)
        }
    }

    fun gep(base: LLVMValueRef, index: LLVMValueRef): LLVMValueRef {
        memScoped {
            val args = allocArrayOf(index)
            return LLVMBuildGEP(builder, base, args[0].ptr, 1, "")!!
        }
    }

    // Only use ignoreOld, when sure that memory is freshly inited and have no value.
    fun updateLocalRef(value: LLVMValueRef, address: LLVMValueRef, ignoreOld: Boolean = false) {
        call(if (ignoreOld) context.llvm.setLocalRefFunction else context.llvm.updateLocalRefFunction,
                listOf(address, value))
    }

    fun updateGlobalRef(value: LLVMValueRef, address: LLVMValueRef, ignoreOld: Boolean = false) {
        call(if (ignoreOld) context.llvm.setGlobalRefFunction else context.llvm.updateGlobalRefFunction,
                listOf(address, value))
    }

    fun isConst(value: LLVMValueRef): Boolean = (LLVMIsConstant(value) == 1)

    //-------------------------------------------------------------------------//

    fun callAtFunctionScope(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>, name: String = "") =
            call(llvmFunction, args, this::cleanupLandingpad, name)

    fun call(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
             lazyLandingpad: () -> LLVMBasicBlockRef? = { null }, name: String = ""): LLVMValueRef {

        memScoped {
            val rargs = allocArrayOf(args)[0].ptr

            if (LLVMIsAFunction(llvmFunction) != null /* the function declaration */ &&
                    LLVMAttribute.LLVMNoUnwindAttribute in LLVMGetFunctionAttrSet(llvmFunction)) {

                return LLVMBuildCall(builder, llvmFunction, rargs, args.size, name)!!
            } else {
                val landingpad = lazyLandingpad()

                if (landingpad == null) {
                    // When calling a function that is not marked as nounwind (can throw an exception),
                    // it is required to specify a landingpad to handle exceptions properly.
                    // Runtime C++ function can be marked as non-throwing using `RUNTIME_NOTHROW`.
                    val functionName = getName(llvmFunction)
                    val message = "no landingpad specified when calling function $functionName without nounwind attr"
                    throw IllegalArgumentException(message)
                }

                val success = basicBlock()
                val result = LLVMBuildInvoke(builder, llvmFunction, rargs, args.size, success, landingpad, name)!!
                positionAtEnd(success)
                return result
            }
        }
    }

    //-------------------------------------------------------------------------//

    fun phi(type: LLVMTypeRef, name: String = ""): LLVMValueRef {
        return LLVMBuildPhi(builder, type, name)!!
    }

    fun addPhiIncoming(phi: LLVMValueRef, vararg incoming: Pair<LLVMBasicBlockRef, LLVMValueRef>) {
        memScoped {
            val incomingValues = allocArrayOf(incoming.map { it.second })
            val incomingBlocks = allocArrayOf(incoming.map { it.first })

            LLVMAddIncoming(phi, incomingValues[0].ptr, incomingBlocks[0].ptr, incoming.size)
        }
    }

    fun assignPhis(vararg phiToValue: Pair<LLVMValueRef, LLVMValueRef>) {
        val currentBlock = this.currentBlock
        phiToValue.forEach {
            addPhiIncoming(it.first, currentBlock to it.second)
        }
    }

    //-------------------------------------------------------------------------//

    /* to class descriptor */
    fun classType(descriptor: ClassDescriptor): LLVMTypeRef = LLVMGetTypeByName(context.llvmModule, descriptor.symbolName)!!
    fun typeInfoValue(descriptor: ClassDescriptor): LLVMValueRef = descriptor.llvmTypeInfoPtr

    /**
     * Pointer to type info for given type, or `null` if the type doesn't have corresponding type info.
     */
    fun typeInfoValue(type: KotlinType): LLVMValueRef? = type.typeInfoPtr?.llvm

    fun param(fn: FunctionDescriptor, i: Int): LLVMValueRef {
        assert (i >= 0 && i < countParams(fn))
        return LLVMGetParam(fn.llvmFunction, i)!!
    }
    fun countParams(fn: FunctionDescriptor) = LLVMCountParams(fn.llvmFunction)

    fun indexInClass(p:PropertyDescriptor):Int = (p.containingDeclaration as ClassDescriptor).fields.indexOf(p)


    fun basicBlock(name: String = "label_"): LLVMBasicBlockRef {
        val currentBlock = this.currentBlock
        val result = LLVMInsertBasicBlock(currentBlock, name)!!
        LLVMMoveBasicBlockAfter(result, currentBlock)
        return result
    }

    fun lastBasicBlock(): LLVMBasicBlockRef? = LLVMGetLastBasicBlock(function)

    fun functionLlvmValue(descriptor: FunctionDescriptor) = descriptor.llvmFunction
    fun functionEntryPointAddress(descriptor: FunctionDescriptor) = descriptor.entryPointAddress.llvm
    fun functionHash(descriptor: FunctionDescriptor): LLVMValueRef = descriptor.functionName.localHash.llvm

    fun br(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        val res = LLVMBuildBr(builder, bbLabel)!!
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun condBr(condition: LLVMValueRef?, bbTrue: LLVMBasicBlockRef?, bbFalse: LLVMBasicBlockRef?): LLVMValueRef? {
        val res = LLVMBuildCondBr(builder, condition, bbTrue, bbFalse)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun ret(value: LLVMValueRef?): LLVMValueRef {
        val res = LLVMBuildBr(builder, epilogueBb)!!

        if (returns.get(currentBlock) != null) {
            // TODO: enable error throwing.
            throw Error("ret() in the same basic block twice!")
        }

        if (value != null)
            returns[currentBlock] = value

        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun  unreachable(): LLVMValueRef? {
        val res = LLVMBuildUnreachable(builder)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    //-------------------------------------------------------------------------//

    fun gxxLandingpad(numClauses: Int, name: String = ""): LLVMValueRef {
        val personalityFunction = LLVMConstBitCast(context.llvm.gxxPersonalityFunction, int8TypePtr)

        // Type of `landingpad` instruction result (depends on personality function):
        val landingpadType = structType(int8TypePtr, int32Type)

        return LLVMBuildLandingPad(builder, landingpadType, personalityFunction, numClauses, name)!!
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the mutable position of instructions being inserted.
     *
     * This class is introduced to workaround unreachable code handling.
     */
    inner class PositionHolder {
        private val builder: LLVMBuilderRef = LLVMCreateBuilder()!!


        fun getBuilder(): LLVMBuilderRef {
            if (isAfterTerminator) {
                positionAtEnd(basicBlock("unreachable"))
            }

            return builder
        }

        /**
         * Should be `true` iff the position is located after terminator instruction.
         */
        var isAfterTerminator: Boolean = false
            private set

        fun setAfterTerminator() {
            isAfterTerminator = true
        }

        val currentBlock: LLVMBasicBlockRef
            get() = LLVMGetInsertBlock(builder)!!

        fun positionAtEnd(block: LLVMBasicBlockRef) {
            LLVMPositionBuilderAtEnd(builder, block)
            val lastInstr = LLVMGetLastInstruction(block)
            isAfterTerminator = lastInstr != null && (LLVMIsATerminatorInst(lastInstr) != null)
        }

        fun dispose() {
            LLVMDisposeBuilder(builder)
        }
    }

    private var currentPositionHolder: PositionHolder = PositionHolder()

    /**
     * Returns `true` iff the current code generation position is located after terminator instruction.
     */
    fun isAfterTerminator() = currentPositionHolder.isAfterTerminator

    val currentBlock: LLVMBasicBlockRef
        get() = currentPositionHolder.currentBlock

    /**
     * The builder representing the current code generation position.
     *
     * Note that it shouldn't be positioned directly using LLVM API due to some hacks.
     * Use e.g. [positionAtEnd] instead. See [PositionHolder] for details.
     */
    val builder: LLVMBuilderRef
        get() = currentPositionHolder.getBuilder()

    fun positionAtEnd(bbLabel: LLVMBasicBlockRef) = currentPositionHolder.positionAtEnd(bbLabel)

    inline fun <R> preservingPosition(code: () -> R): R {
        val oldPositionHolder = currentPositionHolder
        val newPositionHolder = PositionHolder()
        currentPositionHolder = newPositionHolder
        try {
            return code()
        } finally {
            currentPositionHolder = oldPositionHolder
            newPositionHolder.dispose()
        }
    }

    inline fun <R> appendingTo(block: LLVMBasicBlockRef, code: CodeGenerator.() -> R) = preservingPosition {
        positionAtEnd(block)
        code()
    }

    fun  llvmFunction(function: FunctionDescriptor): LLVMValueRef = function.llvmFunction

}


