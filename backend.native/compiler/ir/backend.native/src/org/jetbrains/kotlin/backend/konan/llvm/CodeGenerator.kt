package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.types.KotlinType

internal class CodeGenerator(override val context: Context) : ContextUtils {
    var currentFunction:FunctionDescriptor? = null

    fun prologue(declaration: IrFunction) {
        assert(declaration.body != null)

        val descriptor = declaration.descriptor
        if (currentFunction == descriptor) return
        variableIndex = 0
        currentFunction = declaration.descriptor
        val fn = declaration.descriptor.llvmFunction.getLlvmValue()
        prologueBb = LLVMAppendBasicBlock(fn, "prologue")
        entryBb = LLVMAppendBasicBlock(fn, "entry")
        positionAtEnd(entryBb!!)

        function2variables.put(declaration.descriptor, mutableMapOf())

        var indexOffset = 0
        if (descriptor is ClassConstructorDescriptor
            || descriptor.dispatchReceiverParameter != null
            || descriptor.extensionReceiverParameter != null) {
            val name = "this"
            val type = pointerType(LLVMInt8Type());
            val v = alloca(type, name)
            store(LLVMGetParam(fn, 0)!!, v)
            currentFunction!!.registerVariable(name, v)
            indexOffset = 1;
        }

        descriptor.valueParameters.forEachIndexed { i, descriptor ->
            val name = descriptor.name.asString()
            val type = descriptor.type
            val v = alloca(type, name)
            store(LLVMGetParam(fn, indexOffset + i)!!, v)

            currentFunction!!.registerVariable(name, v)
        }
    }


    fun epilogue(declaration: IrFunction) {
        appendingTo(prologueBb!!) {
            br(entryBb!!)
        }
    }

    fun fields(descriptor: ClassDescriptor):List<PropertyDescriptor> = descriptor.fields

    fun newVar():String = currentFunction!!.tmpVariable()

    val variablesGlobal = mapOf<String, LLVMValueRef?>()
    fun variable(varName:String): LLVMValueRef? = currentFunction!!.variable(varName)

    private var variableIndex:Int = 0
    private var FunctionDescriptor.tmpVariableIndex: Int
        get() = variableIndex
        set(i:Int) { variableIndex = i}

    fun FunctionDescriptor.tmpVariable():String = "tmp_${tmpVariableIndex++}"

    fun registerVariable(varName: String, value: LLVMValueRef) = currentFunction!!.registerVariable(varName, value)

    val function2variables = mutableMapOf<FunctionDescriptor, MutableMap<String, LLVMValueRef?>>()

    private var prologueBb: LLVMBasicBlockRef? = null
    private var entryBb: LLVMBasicBlockRef? = null

    val FunctionDescriptor.variables: MutableMap<String, LLVMValueRef?>
        get() = this@CodeGenerator.function2variables[this]!!

    val ClassConstructorDescriptor.thisValue: LLVMValueRef?
    get() = thisValue


    fun FunctionDescriptor.registerVariable(varName: String, value: LLVMValueRef?) = variables.put(varName, value)
    private fun FunctionDescriptor.variable(varName: String): LLVMValueRef? = variables[varName]

    fun plus  (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildAdd (builder, arg0, arg1, result)!!
    fun mul   (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildMul (builder, arg0, arg1, result)!!
    fun minus (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildSub (builder, arg0, arg1, result)!!
    fun div   (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildSDiv(builder, arg0, arg1, result)!!
    fun srem  (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildSRem(builder, arg0, arg1, result)!!

    /* integers comparisons */
    fun icmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ,  arg0, arg1, result)!!
    fun icmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, result)!!
    fun icmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, result)!!
    fun icmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, result)!!
    fun icmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, result)!!
    fun icmpNe(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntNE,  arg0, arg1, result)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, result)!!

    fun bitcast(type: LLVMTypeRef?, value: LLVMValueRef, result: String) = LLVMBuildBitCast(builder, value, type, result)!!

    fun alloca(type: KotlinType, varName: String): LLVMValueRef = alloca( getLLVMType(type), varName)
    fun alloca(type: LLVMTypeRef?, varName: String): LLVMValueRef {
        appendingTo(prologueBb!!) {
            return LLVMBuildAlloca(builder, type, varName)!!
        }
    }
    fun load(value: LLVMValueRef, varName: String): LLVMValueRef = LLVMBuildLoad(builder, value, varName)!!
    fun store(value: LLVMValueRef, ptr: LLVMValueRef): LLVMValueRef = LLVMBuildStore(builder, value, ptr)!!

    //-------------------------------------------------------------------------//

    fun invoke(llvmFunction: LLVMValueRef?, args: List<LLVMValueRef?>,
               then: LLVMBasicBlockRef, landingpad: LLVMBasicBlockRef,
               result: String?): LLVMValueRef {

        memScoped {
            val rargs = if (args.size != 0) allocArrayOf(args)[0].ptr else null
            return LLVMBuildInvoke(builder, llvmFunction, rargs, args.size, then, landingpad, result)!!
        }

    }

    fun call(llvmFunction: LLVMValueRef?, args: List<LLVMValueRef?>, result: String?): LLVMValueRef {
        memScoped {
            val rargs = if (args.size != 0) allocArrayOf(args)[0].ptr else null
            return LLVMBuildCall(builder, llvmFunction, rargs, args.size, result)!!
        }
    }

    //-------------------------------------------------------------------------//

    fun phi(type: LLVMTypeRef, result: String): LLVMValueRef {
        return LLVMBuildPhi(builder, type, result)!!
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
    fun typeInfoType(descriptor: ClassDescriptor): LLVMTypeRef = descriptor.llvmTypeInfoPtr.getLlvmType()
    fun typeInfoValue(descriptor: ClassDescriptor): LLVMValueRef = descriptor.llvmTypeInfoPtr.getLlvmValue()!!

    fun thisVariable() = variable("this")!!
    fun param(fn: FunctionDescriptor?, i: Int): LLVMValueRef? = LLVMGetParam(fn!!.llvmFunction.getLlvmValue(), i)

    fun indexInClass(p:PropertyDescriptor):Int = (p.containingDeclaration as ClassDescriptor).fields.indexOf(p)


    fun basicBlock(name: String = "label_"): LLVMBasicBlockRef =
            LLVMAppendBasicBlock(currentFunction!!.llvmFunction.getLlvmValue(), name)!!

    fun basicBlock(name: String, code: () -> Unit) = basicBlock(name).apply {
        appendingTo(this) {
            code()
        }
    }

    fun lastBasicBlock(): LLVMBasicBlockRef? = LLVMGetLastBasicBlock(currentFunction!!.llvmFunction.getLlvmValue())

    fun functionLlvmValue(descriptor: FunctionDescriptor) = descriptor.llvmFunction.getLlvmValue()!!
    fun functionHash(descriptor: FunctionDescriptor): LLVMValueRef = descriptor.functionName.localHash.getLlvmValue()!!

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
        val res = LLVMBuildRet(builder, value)!!
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun  unreachable(): LLVMValueRef? {
        val res = LLVMBuildUnreachable(builder)
        currentPositionHolder.setAfterTerminator()
        return res
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

}


