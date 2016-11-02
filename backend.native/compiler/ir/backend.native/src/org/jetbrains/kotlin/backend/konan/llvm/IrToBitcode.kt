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
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetVariableImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid



fun emitLLVM(module: IrModuleFragment, runtimeFile: String, outFile: String) {
    val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    val runtime = Runtime(runtimeFile) // TODO: dispose
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    LLVMSetTarget(llvmModule, runtime.target)

    val context = Context(module, runtime, llvmModule) // TODO: dispose

    module.acceptVoid(RTTIGeneratorVisitor(context))
    println("\n--- Generate bitcode ------------------------------------------------------\n")
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
    val logger = Logger(generator, context)
    val metadator = MetadataGenerator(context)

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitWhen(expression: IrWhen) {
        logger.log("visitWhen                  : ${ir2string(expression)}")
        var bbExit:LLVMOpaqueBasicBlock? = null             // By default "when" does not have "exit"
        if (!KotlinBuiltIns.isNothing(expression.type))     // If "when" has "exit".
            bbExit = generator.basicBlock()                 // Create basic block to process "exit".

        expression.branches.forEach {                       // Iterate through "when" branches (clauses).
            var bbNext = bbExit                             // For last clause bbNext coincides with bbExit.
            if (it != expression.branches.last())           // If it is not last clause.
                bbNext = generator.basicBlock()             // Create new basic block for next clause.
            generateWhenCase(it, bbNext, bbExit)            // Generate code for current clause.
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitLoop(loop: IrLoop) {
        TODO()
    }

    //-------------------------------------------------------------------------//

    override fun visitWhileLoop(loop: IrWhileLoop) {
        val loopEnter = generator.basicBlock()
        val loopBody  = generator.basicBlock()
        val loopExit  = generator.basicBlock()

        LLVMBuildBr(context.llvmBuilder, loopEnter)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopEnter)
        val condition = evaluateExpression(generator.tmpVariable(), loop.condition)
        LLVMBuildCondBr(context.llvmBuilder, condition, loopBody, loopExit)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopBody)
        evaluateExpression(generator.tmpVariable(), loop.body)

        LLVMBuildBr(context.llvmBuilder, loopEnter)
        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopExit)
    }

    //-------------------------------------------------------------------------//

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        val loopBody  = generator.basicBlock()
        val loopCheck = generator.basicBlock()
        val loopExit  = generator.basicBlock()

        LLVMBuildBr(context.llvmBuilder, loopBody)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopBody)
        evaluateExpression(generator.tmpVariable(), loop.body)
        LLVMBuildBr(context.llvmBuilder, loopCheck)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopCheck)
        val condition = evaluateExpression(generator.tmpVariable(), loop.condition)
        LLVMBuildCondBr(context.llvmBuilder, condition, loopBody, loopExit)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopExit)
    }

    //-------------------------------------------------------------------------//

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
        logger.log("visitConstructor           : ${ir2string(declaration)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)
        if (KotlinBuiltIns.isUnit(generator.currentFunction!!.returnType!!)) {
            LLVMBuildRet(context.llvmBuilder, null)
        }
        logger.log("visitBlockBody             : ${ir2string(body)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        evaluateCall(generator.tmpVariable(), expression)
    }

   //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall) {
        logger.log("visitCall ${ir2string(expression)}")
        val isUnit = KotlinBuiltIns.isUnit(expression.descriptor.returnType!!)
        val tmpVariable = if (isUnit) "" else generator.tmpVariable()
        evaluateExpression(tmpVariable, expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitFunction(declaration: IrFunction) {
        generator.function(declaration)
        metadator.function(declaration)
        declaration.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any code for annotation classes as a workaround for NotImplementedError
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }
    }

    //-------------------------------------------------------------------------//
    // Create new variable in LLVM ir

    override fun visitVariable(declaration: IrVariable) {
        logger.log("visitVariable              : ${ir2string(declaration)}")
        val variableName = declaration.descriptor.name.asString()
        val variableType = declaration.descriptor.type
        val newVariable  = generator.alloca(variableType, variableName)        // Create LLVM variable.
        generator.registerVariable(variableName, newVariable)                  // Map variableName -> LLVM variable.
        val value = evaluateExpression(variableName, declaration.initializer)  // Generate initialization code.
        generator.store(value!!, generator.variable(variableName)!!)           // Store init result in the variable
    }

    //-------------------------------------------------------------------------//

    override fun visitReturn(expression: IrReturn) {
        logger.log("visitReturn                : ${ir2string(expression)}")
        val tmpVarName = generator.tmpVariable()                            // Generate new tmp name.
        val value      = evaluateExpression(tmpVarName, expression.value)   //
        LLVMBuildRet(context.llvmBuilder, value)
    }

    //-------------------------------------------------------------------------//

    override fun visitSetVariable(expression: IrSetVariable) {
        logger.log("visitSetVariable           : ${ir2string(expression)}")
        val value = evaluateExpression(generator.tmpVariable(), expression.value)
        generator.store(value!!, generator.variable(expression.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(tmpVariableName: String, value: IrElement?): LLVMOpaqueValue? {
        when (value) {
            is IrCall            -> return evaluateCall           (tmpVariableName, value)
            is IrGetValue        -> return evaluateGetValue       (tmpVariableName, value)
            is IrSetVariable     -> return evaluateSetVariable    (                 value)
            is IrVariable        -> return evaluateVariable       (                 value)
            is IrGetField        -> return evaluateGetField       (                 value)
            is IrConst<*>        -> return evaluateConst          (                 value)
            is IrReturn          -> return evaluateReturn         (                 value)
            is IrBlock           -> return evaluateBlock          (                 value)
            null                 -> return null
            else                 -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateCall(tmpVariableName: String, value: IrMemberAccessExpression?): LLVMOpaqueValue? {
        logger.log("evaluateCall               : $tmpVariableName = ${ir2string(value)}")
        val args = mutableListOf<LLVMOpaqueValue?>()                            // Create list of function args.
        value!!.acceptChildrenVoid(object: IrElementVisitorVoid {               // Iterate args of the function.
            override fun visitElement(element: IrElement) {                     // Visit arg.
                val tmp = generator.tmpVariable()                               // Create variable representing the arg in generator
                args.add(evaluateExpression(tmp, element as IrExpression))      // Evaluate expression and get LLVM arg
            }
        })

        when {
            value.descriptor is FunctionDescriptor -> return evaluateFunctionCall(tmpVariableName, value as IrCall, args)
            value is IrDelegatingConstructorCall -> return generator.superCall(tmpVariableName, value.descriptor, args)
            else -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetValue(tmpVariableName: String, value: IrGetValue): LLVMOpaqueValue {
        logger.log("evaluateGetValue           : $tmpVariableName = ${ir2string(value)}")
        when (value.descriptor) {
            is LocalVariableDescriptor,
            is ValueParameterDescriptor,
            is IrTemporaryVariableDescriptor -> {
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

    //-------------------------------------------------------------------------//

    private fun evaluateSetVariable(value: IrSetVariable): LLVMOpaqueValue {
        logger.log("evaluateSetVariable        : ${ir2string(value)}")
        val ret = evaluateExpression(generator.tmpVariable(), value.value)
        return generator.store(ret!!, generator.variable(value.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateVariable(value: IrVariable): LLVMOpaqueValue {
        logger.log("evaluateVariable           : ${ir2string(value)}")
        val ret = evaluateExpression(generator.tmpVariable(), value.initializer)
        val variableName = value.descriptor.name.asString()
        val variable = generator.alloca(LLVMTypeOf(ret), variableName)
        generator.registerVariable(variableName, variable)
        return generator.store(ret!!, variable)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetField(value: IrGetField): LLVMOpaqueValue {
        logger.log("evaluateGetField           : ${ir2string(value)}")
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = generator.load(generator.thisVariable(), generator.tmpVariable())
            val typedPtr = generator.bitcast(pointerType(generator.classType(generator.currentClass!!)), thisPtr, generator.tmpVariable())
            val fieldPtr = LLVMBuildStructGEP(generator.context.llvmBuilder, typedPtr, generator.indexInClass(value.descriptor), generator.tmpVariable())
            return generator.load(fieldPtr!!, generator.tmpVariable())
        }
        TODO()
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConst(value: IrConst<*>): LLVMOpaqueValue? {
        logger.log("evaluateConst              : ${ir2string(value)}")
        when (value.kind) {
            IrConstKind.Null -> TODO() // LLVMConstPointerNull
            IrConstKind.Boolean -> when (value.value) {
                true  -> return LLVMConstInt(LLVMInt1Type(), 1, 1)
                false -> return LLVMConstInt(LLVMInt1Type(), 0, 1)
            }
            IrConstKind.Char   -> TODO()
            IrConstKind.Byte   -> return LLVMConstInt(LLVMInt32Type(), (value.value as Byte).toLong(), 1)
            IrConstKind.Short  -> return LLVMConstInt(LLVMInt32Type(), (value.value as Short).toLong(), 1)
            IrConstKind.Int    -> return LLVMConstInt(LLVMInt32Type(), (value.value as Int).toLong(), 1)
            IrConstKind.Long   -> return LLVMConstInt(LLVMInt64Type(), value.value as Long, 1)
            IrConstKind.String ->
                return context.staticData.createStringLiteral(value as IrConst<String>).getLlvmValue()
            IrConstKind.Float  -> TODO()
            IrConstKind.Double -> TODO()
        }
        TODO()
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturn(value: IrReturn): LLVMOpaqueValue? {
        logger.log("evaluateReturn             : ${ir2string(value)}")
        val ret = evaluateExpression(generator.tmpVariable(), value.value)
        return LLVMBuildRet(context.llvmBuilder, ret)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateBlock(value: IrBlock): LLVMOpaqueValue? {
        logger.log("evaluateBlock              : ${ir2string(value)}")
        value.statements.dropLast(1).forEach {
            evaluateExpression(generator.tmpVariable(), it)
        }
        return evaluateExpression(generator.tmpVariable(), value.statements.lastOrNull())
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(tmpVariableName: String, value: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        logger.log("evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}")
        return generator.call(value.descriptor as FunctionDescriptor, args, tmpVariableName)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(tmpVariableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        val descriptor:FunctionDescriptor = callee.descriptor as FunctionDescriptor
        when {
            descriptor.isOperator                         -> return evaluateOperatorCall(tmpVariableName, callee.origin!!, args)
            descriptor is IrBuiltinOperatorDescriptorBase -> return evaluateOperatorCall(tmpVariableName, callee.origin!!, args)
            descriptor is ClassConstructorDescriptor      -> return evaluateConstructorCall(tmpVariableName, callee, args)
            else                                          -> return evaluateSimpleFunctionCall(tmpVariableName, callee, args)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConstructorCall(variableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        logger.log("evaluateConstructorCall    : $variableName = ${ir2string(callee)}")
        memScoped {
            val params = allocNativeArrayOf(LLVMOpaqueValue, generator.typeInfoValue((callee.descriptor as ClassConstructorDescriptor).containingDeclaration), Int32(1).getLlvmValue())
            val thisValue = LLVMBuildCall(context.llvmBuilder, context.allocInstanceFunction, params[0], 2, variableName)

            val constructorParams: MutableList<LLVMOpaqueValue?> = mutableListOf()
            constructorParams += thisValue
            constructorParams += args
            return generator.call(callee.descriptor as FunctionDescriptor, constructorParams, variableName)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateOperatorCall(tmpVariableName: String, origin: IrStatementOrigin, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue {
        logger.log("evaluateCall $tmpVariableName origin:$origin")
        when (origin) {
            IrStatementOrigin.PLUS     -> return generator.plus  (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.MINUS    -> return generator.minus (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.PLUSEQ   -> return generator.plus  (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.MINUSEQ  -> return generator.minus (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.MUL      -> return generator.mul   (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.DIV      -> return generator.div   (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.PERC     -> return generator.srem  (args[0]!!, args[1]!!, tmpVariableName)
            IrStatementOrigin.EQEQ     -> return generator.icmpEq(args[0]!!, args[1]!!, tmpVariableName)
            else -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun generateWhenCase(branch: IrBranch, bbNext: LLVMOpaqueBasicBlock?, bbExit: LLVMOpaqueBasicBlock?) {
        if (isUnconditional(branch)) {                                                      // It is the "else" clause.
            evaluateExpression(generator.tmpVariable(), branch.result)                      // Generate clause body.
            if (bbExit == null) return                                                      // If "when" does not have exit - return.
            LLVMBuildBr(context.llvmBuilder, bbExit)                                        // Generate branch to bbExit.
            LLVMPositionBuilderAtEnd(context.llvmBuilder, bbExit)                           // Switch generation to bbExit.
        } else {                                                                            // It is conditional clause.
            val bbCurr = generator.basicBlock()                                             // Create block for clause body.
            val condition = evaluateExpression(generator.tmpVariable(), branch.condition)   // Generate cmp instruction.
            LLVMBuildCondBr(context.llvmBuilder, condition, bbCurr, bbNext)                 // Conditional branch depending on cmp result.
            LLVMPositionBuilderAtEnd(context.llvmBuilder, bbCurr)                           // Switch generation to block for clause body.
            evaluateExpression(generator.tmpVariable(), branch.result)                      // Generate clause body.
            if (!KotlinBuiltIns.isNothing(branch.result.type))                              // If clause code does not contain "return".
                LLVMBuildBr(context.llvmBuilder, bbExit)                                    // Generate branch to bbExit.
            LLVMPositionBuilderAtEnd(context.llvmBuilder, bbNext)                           // Switch generation to bbNextClause.
        }
    }

    //-------------------------------------------------------------------------//
    // Checks if the branch is unconditional

    private fun isUnconditional(branch: IrBranch): Boolean =
        branch.condition is IrConst<*>                            // If branch condition is constant.
            && (branch.condition as IrConst<*>).value as Boolean  // If condition is "true"

}
