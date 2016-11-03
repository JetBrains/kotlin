package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.backend.native.isIntrinsic
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe


fun emitLLVM(module: IrModuleFragment, runtimeFile: String, outFile: String) {
    val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    val runtime = Runtime(runtimeFile) // TODO: dispose
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    LLVMSetTarget(llvmModule, runtime.target)

    val context = Context(module, runtime, llvmModule) // TODO: dispose

    module.acceptVoid(RTTIGeneratorVisitor(context))
    module.acceptVoid(CodeGeneratorVisitor(context))
    memScoped {
        val errorRef = alloc(Int8Box.ref)
        // TODO: use LLVMDisposeMessage() on errorRef, once possible in interop.
        if (LLVMVerifyModule(
                llvmModule, LLVMVerifierFailureAction.LLVMPrintMessageAction, errorRef) == 1) {
            LLVMDumpModule(llvmModule)
            throw Error("Invalid module");
        }
    }
    LLVMWriteBitcodeToFile(llvmModule, outFile)
}

internal class RTTIGeneratorVisitor(context: Context) : IrElementVisitorVoid {
    val generator = RTTIGenerator(context)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any RTTI for annotation classes as a workaround for link errors
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        generator.generate(declaration.descriptor)
    }

}

internal class CodeGeneratorVisitor(val context: Context) : IrElementVisitorVoid {

    val generator = CodeGenerator(context)

    override fun visitConstructor(declaration: IrConstructor) {
        generator.initFunction(declaration)
        val thisValue = generator.variable("this")
        //super.visitConstructor(declaration)
        /**
         *   %this = alloca i8*
         *   store i8* %0, i8** %this <- prolog
         *
         *   %tmp0 = load i8*, i8** %this <- epilog
         *   ret i8* %tmp0
         */
        LLVMBuildRet(context.llvmBuilder, generator.load(thisValue!!, generator.tmpVariable()))
    }

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)
        if (KotlinBuiltIns.isUnit(generator.currentFunction!!.returnType!!)) {
            LLVMBuildRet(context.llvmBuilder, null)
        }
    }
    
    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        evaluateCall(generator.tmpVariable(), expression)
    }

    override fun visitCall(expression: IrCall) {
        val isUnit = KotlinBuiltIns.isUnit(expression.descriptor.returnType!!)
        val tmpVariable = if (isUnit) "" else generator.tmpVariable()
        evaluateExpression(tmpVariable, expression)
    }

    override fun visitFunction(declaration: IrFunction) {
        generator.function(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any code for annotation classes as a workaround for NotImplementedError
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        super.visitClass(declaration)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        val value = evaluateExpression(generator.tmpVariable(), expression.value)
        generator.store(value!!, generator.variable(expression.descriptor.name.asString())!!)
    }

    private fun evaluateExpression(tmpVariableName: String, value: IrExpression?): LLVMOpaqueValue? {
        when (value) {
            is IrCall -> return evaluateCall(tmpVariableName, value)
            is IrGetValue -> {
                when (value.descriptor) {
                    is LocalVariableDescriptor, is ValueParameterDescriptor -> {
                        val variable = generator.variable(value.descriptor.name.asString())
                        return generator.load(variable!!, tmpVariableName)
                    }
                    is LazyClassReceiverParameterDescriptor -> {
                        if (value.descriptor.name.asString() == "<this>") {
                            return generator.load(generator.thisVariable(), tmpVariableName)
                        }
                        TODO()
                    }
                    else -> {
                        TODO()
                    }
                }

            }
            is IrGetField -> {
                if (value.descriptor.dispatchReceiverParameter != null) {
                    val thisPtr =  generator.load(generator.thisVariable(), generator.tmpVariable())
                    val typedPtr = generator.bitcast(pointerType(generator.classType(generator.currentClass!!)), thisPtr, generator.tmpVariable())
                    val fieldPtr = LLVMBuildStructGEP(generator.context.llvmBuilder, typedPtr, generator.indexInClass(value.descriptor), generator.tmpVariable())
                    return generator.load(fieldPtr!!, generator.tmpVariable())
                }
                TODO()
            }

            is IrConst<*> -> when (value.kind) {
                IrConstKind.Null    -> TODO() // LLVMConstPointerNull
                IrConstKind.Boolean -> TODO()
                IrConstKind.Char    -> TODO()
                IrConstKind.Byte    -> return LLVMConstInt(LLVMInt32Type(), (value.value as Byte).toLong(),  1)
                IrConstKind.Short   -> return LLVMConstInt(LLVMInt32Type(), (value.value as Short).toLong(), 1)
                IrConstKind.Int     -> return LLVMConstInt(LLVMInt32Type(), (value.value as Int).toLong(),   1)
                IrConstKind.Long    -> return LLVMConstInt(LLVMInt64Type(),  value.value as Long,            1)
                IrConstKind.String  -> TODO()
                IrConstKind.Float   -> TODO()
                IrConstKind.Double  -> TODO()
            }

            null -> return null
            else -> {
                TODO()
            }
        }
    }


    private fun evaluateCall(tmpVariableName: String, value: IrMemberAccessExpression?): LLVMOpaqueValue? {
        val args = mutableListOf<LLVMOpaqueValue?>()
        value!!.acceptChildrenVoid(object:IrElementVisitorVoid{
            override fun visitElement(element: IrElement) {
                val tmp = generator.tmpVariable()
                args.add(evaluateExpression(tmp, element as IrExpression))
            }
        })
        when {
            value is IrDelegatingConstructorCall -> return generator.superCall(tmpVariableName, value.descriptor, args)
            value.descriptor is FunctionDescriptor -> return evaluateFunctionCall(tmpVariableName, value as IrCall, args)
            else -> {
                TODO()
            }
        }

        TODO()
    }

    private fun evaluateSimpleFunctionCall(tmpVariableName: String, value: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        return generator.call(value.descriptor as FunctionDescriptor, args, tmpVariableName)
    }


    private fun evaluateFunctionCall(tmpVariableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        val descriptor:FunctionDescriptor = callee.descriptor as FunctionDescriptor
        when {
            descriptor.isOperator -> return evaluateOperatorCall(tmpVariableName, callee, args)
            descriptor is ClassConstructorDescriptor -> return evaluateConstructorCall(tmpVariableName, callee, args)
            else -> {
                return evaluateSimpleFunctionCall(tmpVariableName, callee, args)
            }
        }
    }

    private fun evaluateConstructorCall(variableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        memScoped {
            val params = allocNativeArrayOf(LLVMOpaqueValue, generator.typeInfoValue((callee.descriptor as ClassConstructorDescriptor).containingDeclaration), Int32(1).getLlvmValue())
            val thisValue = LLVMBuildCall(context.llvmBuilder, context.allocInstanceFunction, params[0], 2, variableName)


            val constructorParams: MutableList<LLVMOpaqueValue?> = mutableListOf()
            constructorParams += thisValue
            constructorParams += args
            return generator.call(callee.descriptor as FunctionDescriptor, constructorParams, variableName)
        }
    }

    private fun evaluateOperatorCall(tmpVariableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue {
        when (callee!!.origin) {
            IrStatementOrigin.PLUS -> return generator.plus(args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.MINUS -> return generator.minus(args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.MUL -> return generator.mul(args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.DIV -> return generator.div(args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.PERC -> return generator.srem(args[0]!!, args[1]!!, tmpVariableName)
            else -> {
                TODO()
            }
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        val variableName = declaration.descriptor.name.asString()
        val variableType = declaration.descriptor.type
        generator.registerVariable(variableName, generator.alloca(variableType, variableName))

        evaluateExpression(variableName, declaration.initializer)
    }

    override fun visitReturn(expression: IrReturn) {
        val tmpVarName = generator.tmpVariable()
        val value:LLVMOpaqueValue?
        value = evaluateExpression(tmpVarName, expression.value)
        LLVMBuildRet(context.llvmBuilder, value)
    }
}
