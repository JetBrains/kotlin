package org.jetbrains.kotlin.backend.konan.llvm


import kotlin_.cinterop.*
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

    private fun prolog(declaration: IrFunction): LLVMOpaqueValue? {
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

    val variablesGlobal = mapOf<String, LLVMOpaqueValue?>()
    fun variable(varName:String):LLVMOpaqueValue? = currentFunction!!.variable(varName)

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

    fun registerVariable(varName: String, value:LLVMOpaqueValue) = currentFunction!!.registerVariable(varName, value)

    val function2variables = mutableMapOf<FunctionDescriptor, MutableMap<String, LLVMOpaqueValue?>>()


    val FunctionDescriptor.variables: MutableMap<String, LLVMOpaqueValue?>
        get() = this@CodeGenerator.function2variables[this]!!

    val ClassConstructorDescriptor.thisValue:LLVMOpaqueValue?
    get() = thisValue


    fun FunctionDescriptor.registerVariable(varName: String, value:LLVMOpaqueValue?) = variables.put(varName, value)
    private fun FunctionDescriptor.variable(varName: String): LLVMOpaqueValue? = variables[varName]

    fun plus  (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildAdd (context.llvmBuilder, arg0, arg1, result)!!
    fun mul   (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildMul (context.llvmBuilder, arg0, arg1, result)!!
    fun minus (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSub (context.llvmBuilder, arg0, arg1, result)!!
    fun div   (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSDiv(context.llvmBuilder, arg0, arg1, result)!!
    fun srem  (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSRem(context.llvmBuilder, arg0, arg1, result)!!

    /* integers comparisons */
    fun icmpEq(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntEQ,  arg0, arg1, result)!!
    fun icmpGt(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, result)!!
    fun icmpGe(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, result)!!
    fun icmpLt(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, result)!!
    fun icmpLe(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, result)!!
    fun icmpNe(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildICmp(context.llvmBuilder, LLVMIntPredicate.LLVMIntNE,  arg0, arg1, result)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildFCmp(context.llvmBuilder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, result)!!

    fun bitcast(type: LLVMOpaqueType?, value: LLVMOpaqueValue, result: String) = LLVMBuildBitCast(context.llvmBuilder, value, type, result)

    fun alloca(type: KotlinType, varName: String):LLVMOpaqueValue = alloca( getLLVMType(type), varName)
    fun alloca(type: LLVMOpaqueType?, varName: String): LLVMOpaqueValue = LLVMBuildAlloca(context.llvmBuilder, type, varName)!!
    fun load(value:LLVMOpaqueValue, varName: String):LLVMOpaqueValue = LLVMBuildLoad(context.llvmBuilder, value, varName)!!
    fun store(value:LLVMOpaqueValue, ptr:LLVMOpaqueValue):LLVMOpaqueValue = LLVMBuildStore(context.llvmBuilder, value, ptr)!!

    //-------------------------------------------------------------------------//

    fun call(llvmFunction: LLVMOpaqueValue?, args: List<LLVMOpaqueValue?>, result: String?): LLVMOpaqueValue? {
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
    fun classType(descriptor: ClassDescriptor):LLVMOpaqueType = LLVMGetTypeByName(context.llvmModule, descriptor.symbolName)!!
    fun typeInfoType(descriptor: ClassDescriptor): LLVMOpaqueType? = descriptor.llvmTypeInfoPtr.getLlvmType()
    fun typeInfoValue(descriptor: ClassDescriptor): LLVMOpaqueValue? = descriptor.llvmTypeInfoPtr.getLlvmValue()

    fun thisVariable() = variable("this")!!
    fun param(fn: FunctionDescriptor?, i: Int): LLVMOpaqueValue? = LLVMGetParam(fn!!.llvmFunction.getLlvmValue(), i)

    fun indexInClass(p:PropertyDescriptor):Int = (p.containingDeclaration as ClassDescriptor).fields.indexOf(p)


    fun basicBlock(): LLVMOpaqueBasicBlock? = LLVMAppendBasicBlock(currentFunction!!.llvmFunction.getLlvmValue(), currentFunction!!.bbLabel())
    fun lastBasicBlock(): LLVMOpaqueBasicBlock? = LLVMGetLastBasicBlock(currentFunction!!.llvmFunction.getLlvmValue())

    fun functionLlvmValue(descriptor: FunctionDescriptor) = descriptor.llvmFunction.getLlvmValue()
    fun functionHash(descriptor: FunctionDescriptor): LLVMOpaqueValue? = descriptor.functionName.localHash.getLlvmValue()

    fun  br(bbLabel: LLVMOpaqueBasicBlock) = LLVMBuildBr(context.llvmBuilder, bbLabel)
    fun condBr(condition: LLVMOpaqueValue?, bbTrue: LLVMOpaqueBasicBlock?, bbFalse: LLVMOpaqueBasicBlock?)
        = LLVMBuildCondBr(context.llvmBuilder, condition, bbTrue, bbFalse)

    fun positionAtEnd(bbLabel: LLVMOpaqueBasicBlock)
        = LLVMPositionBuilderAtEnd(context.llvmBuilder, bbLabel)

    fun ret(value: LLVMOpaqueValue?) = LLVMBuildRet(context.llvmBuilder, value)
}


