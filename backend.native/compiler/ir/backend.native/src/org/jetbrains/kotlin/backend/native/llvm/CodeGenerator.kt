package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.*
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
        if (declaration.descriptor.isExternal) {
            return
        }

        if (currentFunction == declaration.descriptor) return
        val fn = prolog(declaration)

        // TODO: functions without Kotlin body must not have a bitcode body,
        // which is required to workaround link errors
        if (declaration.body == null) {
            trap()
            return
        }

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

    /* HACK: */
    var currentClass:ClassDescriptor? = null
    /* constructor */
    fun initFunction(declaration: IrConstructor) {
        function(declaration)
        val thisPtr = bitcast(pointerType(classType(declaration.descriptor.containingDeclaration)), load(thisVariable(), tmpVariable()), tmpVariable())

        /**
         * TODO: check shadowing.
         */
        declaration.descriptor.containingDeclaration.fields.forEachIndexed { i, descriptor ->
            val name = descriptor.name.asString()

            if (!declaration.descriptor.valueParameters.any { it -> it.name.asString() == name })
                return@forEachIndexed

            val ptr = LLVMBuildStructGEP(context.llvmBuilder, thisPtr, i, tmpVariable())
            val value = load(variable(name)!!, tmpVariable())

            val typePtr = bitcast(pointerType(LLVMTypeOf(value)), ptr!!, tmpVariable())
            store(value, typePtr!!)
        }
        currentClass = declaration.descriptor.constructedClass
    }


    private fun prolog(declaration: IrFunction): LLVMOpaqueValue? {
        index = 0
        currentFunction = declaration.descriptor
        val fn = declaration.descriptor.llvmFunction.getLlvmValue()
        val block = LLVMAppendBasicBlock(fn, "entry")
        LLVMPositionBuilderAtEnd(context.llvmBuilder, block)
        function2variables.put(declaration.descriptor, mutableMapOf())
        return fn
    }

    fun tmpVariable():String = currentFunction!!.tmpVariable()

    val variablesGlobal = mapOf<String, LLVMOpaqueValue?>()
    fun variable(varName:String):LLVMOpaqueValue? = currentFunction!!.variable(varName)

    var index:Int = 0
    private var FunctionDescriptor.tmpVariableIndex: Int
        get() = index
        set(i:Int){ index = i}

    fun FunctionDescriptor.tmpVariable():String = "tmp${tmpVariableIndex++}"

    fun registerVariable(varName: String, value:LLVMOpaqueValue) = currentFunction!!.registerVariable(varName, value)

    val function2variables = mutableMapOf<FunctionDescriptor, MutableMap<String, LLVMOpaqueValue?>>()


    val FunctionDescriptor.variables: MutableMap<String, LLVMOpaqueValue?>
        get() = this@CodeGenerator.function2variables[this]!!

    val ClassConstructorDescriptor.thisValue:LLVMOpaqueValue?
    get() = thisValue


    fun FunctionDescriptor.registerVariable(varName: String, value:LLVMOpaqueValue?) = variables.put(varName, value)
    private fun FunctionDescriptor.variable(varName: String): LLVMOpaqueValue? = variables[varName]

    fun plus (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildAdd (context.llvmBuilder, arg0, arg1, result)!!
    fun mul  (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildMul (context.llvmBuilder, arg0, arg1, result)!!
    fun minus(arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSub (context.llvmBuilder, arg0, arg1, result)!!
    fun div  (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSDiv(context.llvmBuilder, arg0, arg1, result)!!
    fun srem (arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, result: String): LLVMOpaqueValue = LLVMBuildSRem(context.llvmBuilder, arg0, arg1, result)!!

    fun bitcast(type: LLVMOpaqueType?, value: LLVMOpaqueValue, result: String) = LLVMBuildBitCast(context.llvmBuilder, value, type, result)

    fun alloca(type: KotlinType, varName: String):LLVMOpaqueValue = alloca( getLLVMType(type), varName)
    fun alloca(type: LLVMOpaqueType?, varName: String): LLVMOpaqueValue = LLVMBuildAlloca(context.llvmBuilder, type, varName)!!
    fun load(value:LLVMOpaqueValue, varName: String):LLVMOpaqueValue = LLVMBuildLoad(context.llvmBuilder, value, varName)!!
    fun store(value:LLVMOpaqueValue, ptr:LLVMOpaqueValue):LLVMOpaqueValue = LLVMBuildStore(context.llvmBuilder, value, ptr)!!

    fun call(descriptor: FunctionDescriptor, args: MutableList<LLVMOpaqueValue?>, result: String): LLVMOpaqueValue? {
        if (args.size == 0) return LLVMBuildCall(context.llvmBuilder, descriptor.llvmFunction.getLlvmValue(), null, 0, result)
        memScoped {
            val rargs = alloc(array[args.size](Ref to LLVMOpaqueValue))
            args.forEachIndexed { i, llvmOpaqueValue ->  rargs[i].value = args[i]}
            return LLVMBuildCall(context.llvmBuilder, descriptor.llvmFunction.getLlvmValue(), rargs[0], args.size, result)
	    }
    }

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

    fun  superCall(result:String, descriptor:ClassConstructorDescriptor, args:MutableList<LLVMOpaqueValue?> ):LLVMOpaqueValue? {
        val tmp = load(thisVariable(), tmpVariable())
        var rargs:MutableList<LLVMOpaqueValue?>? = null
        if (args.size != 0)
            rargs = mutableListOf<LLVMOpaqueValue?>(tmp, *args.toTypedArray())
        else
            rargs = mutableListOf<LLVMOpaqueValue?>(tmp)
        return call(descriptor, rargs, result)
    }

    fun thisVariable() = variable("this")!!
    fun param(fn: FunctionDescriptor?, i: Int): LLVMOpaqueValue? = LLVMGetParam(fn!!.llvmFunction.getLlvmValue(), i)

    fun indexInClass(p:PropertyDescriptor):Int = currentClass!!.fields.indexOf(p)
}


