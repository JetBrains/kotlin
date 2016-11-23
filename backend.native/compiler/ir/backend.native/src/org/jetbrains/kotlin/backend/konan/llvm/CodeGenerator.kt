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

internal class CodeGenerator(override val context:Context) : ContextUtils {
    var currentFunction:FunctionDescriptor? = null

    fun function(declaration: IrFunction) {
        if (declaration.body == null) {
            // Abstract and external methods.
            return
        }

        if (currentFunction == declaration.descriptor) return
        val fn = prolog(declaration)

        var indexOffset = 0
        if (declaration.descriptor is ClassConstructorDescriptor || declaration.descriptor.dispatchReceiverParameter != null) {
            val name = "this"
            val type = pointerType(LLVMInt8Type());
            val v = alloca(type, name)
            store(LLVMGetParam(fn, 0)!!, v)
            currentFunction!!.registerVariable(name, v)
            indexOffset = 1;
        }

        declaration.descriptor.valueParameters.forEachIndexed { i, descriptor ->
            val name = descriptor.name.asString()
            val type = descriptor.type
            val v = alloca(type, name)
            store(LLVMGetParam(fn, indexOffset + i)!!, v)

            currentFunction!!.registerVariable(name, v)
        }
    }

    fun  fields(descriptor: ClassDescriptor):List<PropertyDescriptor> = descriptor.fields

    private fun prolog(declaration: IrFunction): LLVMValueRef? {
        variableIndex = 0
        labelIndex = 0
        currentFunction = declaration.descriptor
        val fn = declaration.descriptor.llvmFunction.getLlvmValue()
        val block = LLVMAppendBasicBlock(fn, "entry")
        LLVMPositionBuilderAtEnd(context.llvmBuilder, block)
        function2variables.put(declaration.descriptor, mutableMapOf())
        return fn
    }

    fun newVar():String = currentFunction!!.tmpVariable()

    val variablesGlobal = mapOf<String, LLVMValueRef?>()
    fun variable(varName:String): LLVMValueRef? = currentFunction!!.variable(varName)

    private var variableIndex:Int = 0
    private var FunctionDescriptor.tmpVariableIndex: Int
        get() = variableIndex
        set(i:Int) { variableIndex = i}

    private var labelIndex:Int = 0
    private var FunctionDescriptor.bbLabelIndex: Int
        get() = labelIndex
        set(i:Int) { labelIndex = i}

    fun FunctionDescriptor.tmpVariable():String = "tmp_${tmpVariableIndex++}"
    fun FunctionDescriptor.bbLabel():String = "label_${bbLabelIndex++}"

    fun registerVariable(varName: String, value: LLVMValueRef) = currentFunction!!.registerVariable(varName, value)

    val function2variables = mutableMapOf<FunctionDescriptor, MutableMap<String, LLVMValueRef?>>()


    val FunctionDescriptor.variables: MutableMap<String, LLVMValueRef?>
        get() = this@CodeGenerator.function2variables[this]!!

    val ClassConstructorDescriptor.thisValue: LLVMValueRef?
    get() = thisValue


    fun FunctionDescriptor.registerVariable(varName: String, value: LLVMValueRef?) = variables.put(varName, value)
    private fun FunctionDescriptor.variable(varName: String): LLVMValueRef? = variables[varName]

    fun plus  (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildAdd (context.llvmBuilder, arg0, arg1, result)!!
    fun mul   (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildMul (context.llvmBuilder, arg0, arg1, result)!!
    fun minus (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildSub (context.llvmBuilder, arg0, arg1, result)!!
    fun div   (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildSDiv(context.llvmBuilder, arg0, arg1, result)!!
    fun srem  (arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildSRem(context.llvmBuilder, arg0, arg1, result)!!

    /* integers comparisons */
    fun icmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntEQ,  arg0, arg1, result)!!
    fun icmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, result)!!
    fun icmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, result)!!
    fun icmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, result)!!
    fun icmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, result)!!
    fun icmpNe(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntNE,  arg0, arg1, result)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, result: String): LLVMValueRef = LLVMBuildFCmp(context.llvmBuilder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, result)!!

    fun bitcast(type: LLVMTypeRef?, value: LLVMValueRef, result: String) = LLVMBuildBitCast(context.llvmBuilder, value, type, result)

    fun alloca(type: KotlinType, varName: String): LLVMValueRef = alloca( getLLVMType(type), varName)
    fun alloca(type: LLVMTypeRef?, varName: String): LLVMValueRef = LLVMBuildAlloca(context.llvmBuilder, type, varName)!!
    fun load(value: LLVMValueRef, varName: String): LLVMValueRef = LLVMBuildLoad(context.llvmBuilder, value, varName)!!
    fun store(value: LLVMValueRef, ptr: LLVMValueRef): LLVMValueRef = LLVMBuildStore(context.llvmBuilder, value, ptr)!!

    //-------------------------------------------------------------------------//

    fun call(llvmFunction: LLVMValueRef?, args: List<LLVMValueRef?>, result: String?): LLVMValueRef? {
        if (args.size == 0) return LLVMBuildCall(context.llvmBuilder, llvmFunction, null, 0, result)
        memScoped {
            val rargs = allocArrayOf(args)
            return LLVMBuildCall(context.llvmBuilder, llvmFunction, rargs[0].ptr, args.size, result)
        }
    }

    //-------------------------------------------------------------------------//

    fun trap() {
        // LLVM doesn't seem to provide API to get intrinsics;
        // workaround this by declaring the intrinsic explicitly:
        val trapFun = LLVMGetNamedFunction(context.llvmModule, "llvm.trap") ?:
                LLVMAddFunction(context.llvmModule, "llvm.trap", LLVMFunctionType(LLVMVoidType(), null, 0, 0))!!

        LLVMBuildCall(context.llvmBuilder, trapFun, null, 0, "")

        // LLVM seems to require an explicit return instruction even after noreturn intrinsic:
        val returnType = LLVMGetReturnType(getFunctionType(currentFunction!!.llvmFunction.getLlvmValue()))
        LLVMBuildRet(context.llvmBuilder, LLVMConstNull(returnType))
    }

    /* to class descriptor */
    fun classType(descriptor: ClassDescriptor): LLVMTypeRef = LLVMGetTypeByName(context.llvmModule, descriptor.symbolName)!!
    fun typeInfoType(descriptor: ClassDescriptor): LLVMTypeRef? = descriptor.llvmTypeInfoPtr.getLlvmType()
    fun typeInfoValue(descriptor: ClassDescriptor): LLVMValueRef? = descriptor.llvmTypeInfoPtr.getLlvmValue()

    fun thisVariable() = variable("this")!!
    fun param(fn: FunctionDescriptor?, i: Int): LLVMValueRef? = LLVMGetParam(fn!!.llvmFunction.getLlvmValue(), i)

    fun indexInClass(p:PropertyDescriptor):Int = (p.containingDeclaration as ClassDescriptor).fields.indexOf(p)


    fun basicBlock(): LLVMBasicBlockRef? = LLVMAppendBasicBlock(currentFunction!!.llvmFunction.getLlvmValue(), currentFunction!!.bbLabel())
    fun lastBasicBlock(): LLVMBasicBlockRef? = LLVMGetLastBasicBlock(currentFunction!!.llvmFunction.getLlvmValue())

    fun functionLlvmValue(descriptor: FunctionDescriptor) = descriptor.llvmFunction.getLlvmValue()
    fun functionHash(descriptor: FunctionDescriptor): LLVMValueRef? = descriptor.functionName.localHash.getLlvmValue()

    fun  br(bbLabel: LLVMBasicBlockRef) = LLVMBuildBr(context.llvmBuilder, bbLabel)
    fun condBr(condition: LLVMValueRef?, bbTrue: LLVMBasicBlockRef?, bbFalse: LLVMBasicBlockRef?)
        = LLVMBuildCondBr(context.llvmBuilder, condition, bbTrue, bbFalse)

    fun positionAtEnd(bbLabel: LLVMBasicBlockRef)
        = LLVMPositionBuilderAtEnd(context.llvmBuilder, bbLabel)

    fun ret(value: LLVMValueRef?) = LLVMBuildRet(context.llvmBuilder, value)
    fun  unreachable(): LLVMValueRef? = LLVMBuildUnreachable(context.llvmBuilder)
}


