package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.types.KotlinType

internal class CodeGenerator(override val context:Context) : ContextUtils {
    var currentFunction:FunctionDescriptor? = null

    fun function(declaration: IrFunction) {
        index = 0
        currentFunction = declaration.descriptor
        val fn = LLVMAddFunction(context.llvmModule, declaration.descriptor.symbolName, getLlvmFunctionType(declaration.descriptor))
        val block = LLVMAppendBasicBlock(fn, "entry")
        LLVMPositionBuilderAtEnd(context.llvmBuilder, block)
        function2variables.put(declaration.descriptor, mutableMapOf())
        declaration.descriptor.valueParameters.forEachIndexed { i, descriptor ->
            val name = descriptor.name.asString()
            val type = descriptor.type
            val v = alloca(type, name)
            store(LLVMGetParam(fn, i)!!, v)
            currentFunction!!.registerVariable(name, v)
        }

    }

    fun tmpVariable():String = currentFunction!!.tmpVariable()

    val variablesGlobal = mapOf<String, LLVMOpaqueValue?>()
    fun variable(varName:String):LLVMOpaqueValue? = currentFunction!!.variable(varName)

    fun FunctionDescriptor.param(num:Int):LLVMOpaqueValue? = LLVMGetParam(llvmFunction.getLlvmValue(), num)

    var index:Int = 0
    private var FunctionDescriptor.tmpVariableIndex: Int
        get() = index
        set(i:Int){ index = i}

    fun FunctionDescriptor.tmpVariable():String = "tmp${tmpVariableIndex++}"

    fun registerVariable(varName: String, value:LLVMOpaqueValue) = currentFunction!!.registerVariable(varName, value)

    val function2variables = mutableMapOf<FunctionDescriptor, MutableMap<String, LLVMOpaqueValue?>>()


    val FunctionDescriptor.variables: MutableMap<String, LLVMOpaqueValue?>
        get() = this@CodeGenerator.function2variables[this]!!


    fun FunctionDescriptor.registerVariable(varName: String, value:LLVMOpaqueValue?) = variables.put(varName, value)
    private fun FunctionDescriptor.variable(varName: String): LLVMOpaqueValue? = variables[varName]

    fun plus(arg0:LLVMOpaqueValue, arg1:LLVMOpaqueValue, result:String):LLVMOpaqueValue = LLVMBuildAdd(context.llvmBuilder, arg0, arg1, result)!!
    fun mul(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildMul(context.llvmBuilder, arg0, arg1, result)!!
    fun minus(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSub(context.llvmBuilder, arg0, arg1, result)!!
    fun div(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSDiv(context.llvmBuilder, arg0, arg1, result)!!
    fun remainder(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSRem(context.llvmBuilder, arg0, arg1, result)!!
    fun alloca(type: KotlinType, varName: String):LLVMOpaqueValue = LLVMBuildAlloca(context.llvmBuilder, getLLVMType(type), varName)!!
    fun load(value:LLVMOpaqueValue, varName: String):LLVMOpaqueValue = LLVMBuildLoad(context.llvmBuilder, value, varName)!!
    fun store(value:LLVMOpaqueValue, ptr:LLVMOpaqueValue):LLVMOpaqueValue = LLVMBuildStore(context.llvmBuilder, value, ptr)!!
    fun call(descriptor: FunctionDescriptor, args: MutableList<LLVMOpaqueValue?>, result: String): LLVMOpaqueValue? {
        val rargs = malloc(array[args.size](Ref to LLVMOpaqueValue))
        args.forEachIndexed { i, llvmOpaqueValue ->  rargs[i].value = args[i]}
        return LLVMBuildCall(context.llvmBuilder, descriptor.llvmFunction.getLlvmValue(), rargs[0], args.size, result)
    }
}


