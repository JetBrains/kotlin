/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization


import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.deserializedPropertyIfAccessor
import org.jetbrains.kotlin.backend.konan.descriptors.isDeserializableCallable
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.llvm.base64Decode
import org.jetbrains.kotlin.backend.konan.llvm.base64Encode
import org.jetbrains.kotlin.backend.konan.lower.DeepCopyIrTreeWithDescriptors
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFINED
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrEnumEntryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.serialization.KonanDescriptorSerializer
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.metadata.KonanIr.IrConst.ValueCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrOperation.OperationCase.*
import org.jetbrains.kotlin.metadata.KonanIr.IrVarargElement.VarargElementCase.*
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor


internal class IrSerializer(val context: Context,
                            descriptorTable: DescriptorTable,
                            stringTable: KonanStringTable,
                            rootFunctionSerializer: KonanDescriptorSerializer,
                            private var rootFunction: FunctionDescriptor) {

    private val loopIndex = mutableMapOf<IrLoop, Int>()
    private var currentLoopIndex = 0
    private val localDeclarationSerializer
        = LocalDeclarationSerializer(context, rootFunctionSerializer)
    private val irDescriptorSerializer
        = IrDescriptorSerializer(context, descriptorTable, 
            stringTable, localDeclarationSerializer, rootFunction)

    fun serializeInlineBody(): String {
        val declaration = context.ir.originalModuleIndex.functions[rootFunction]!!
        context.log{"INLINE: ${ir2stringWhole(declaration)}"}
        return encodeDeclaration(declaration)
    }

    private fun serializeKotlinType(type: KotlinType): KonanIr.KotlinType {
        context.log{"### serializing KotlinType: " + type}
        return irDescriptorSerializer.serializeKotlinType(type)
    }

    private fun serializeKotlinType(type: IrType) = serializeKotlinType(type.toKotlinType())

    private fun serializeDescriptor(descriptor: DeclarationDescriptor): KonanIr.KotlinDescriptor {
        context.log{"### serializeDescriptor $descriptor"}

        // Behind this call starts a large world of 
        // descriptor serialization for IR.
        return irDescriptorSerializer.serializeDescriptor(descriptor)
    }

    private fun serializeCoordinates(start: Int, end: Int): KonanIr.Coordinates {
        return KonanIr.Coordinates.newBuilder()
            .setStartOffset(start)
            .setEndOffset(end)
            .build()
    }

    private fun serializeTypeArguments(call: IrMemberAccessExpression): KonanIr.TypeArguments {
        val proto = KonanIr.TypeArguments.newBuilder()
        for (i in 0 until call.typeArgumentsCount) {
            proto.addTypeArgument(serializeKotlinType(call.getTypeArgument(i)!!))
        }
        return proto.build()
     }

    /* -------------------------------------------------------------------------- */

    private fun serializeBlockBody(expression: IrBlockBody): KonanIr.IrBlockBody {
        val proto = KonanIr.IrBlockBody.newBuilder()
        expression.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeBranch(branch: IrBranch): KonanIr.IrBranch {
        val proto = KonanIr.IrBranch.newBuilder()

        proto.condition = serializeExpression(branch.condition)
        proto.result = serializeExpression(branch.result)

        return proto.build()
    }

    private fun serializeBlock(block: IrBlock): KonanIr.IrBlock {
        val isLambdaOrigin = 
            block.origin == IrStatementOrigin.LAMBDA ||
            block.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val proto = KonanIr.IrBlock.newBuilder()
            .setIsLambdaOrigin(isLambdaOrigin)
        block.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeComposite(composite: IrComposite): KonanIr.IrComposite {
        val proto = KonanIr.IrComposite.newBuilder()
        composite.statements.forEach {
            proto.addStatement(serializeStatement(it))
        }
        return proto.build()
    }

    private fun serializeCatch(catch: IrCatch): KonanIr.IrCatch {
        val proto = KonanIr.IrCatch.newBuilder()
           .setCatchParameter(serializeDeclaration(catch.catchParameter))
           .setResult(serializeExpression(catch.result))
        return proto.build()
    }

    private fun serializeStringConcat(expression: IrStringConcatenation): KonanIr.IrStringConcat {
        val proto = KonanIr.IrStringConcat.newBuilder()
        expression.arguments.forEach {
            proto.addArgument(serializeExpression(it))
        }
        return proto.build()
    }

    private fun irCallToPrimitiveKind(call: IrCall): KonanIr.IrCall.Primitive = when (call) {
        is IrNullaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.NULLARY
        is IrUnaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.UNARY
        is IrBinaryPrimitiveImpl 
            -> KonanIr.IrCall.Primitive.BINARY
        else
            -> KonanIr.IrCall.Primitive.NOT_PRIMITIVE
    }

    private fun serializeMemberAccessCommon(call: IrMemberAccessExpression): KonanIr.MemberAccessCommon {
        val proto = KonanIr.MemberAccessCommon.newBuilder()
        if (call.extensionReceiver != null) {
            proto.extensionReceiver = serializeExpression(call.extensionReceiver!!)
        }

        if (call.dispatchReceiver != null)  {
            proto.dispatchReceiver = serializeExpression(call.dispatchReceiver!!)
        }
        proto.typeArguments = serializeTypeArguments(call)

        call.descriptor.valueParameters.forEach {
            val actual = call.getValueArgument(it.index)
            val argOrNull = KonanIr.NullableIrExpression.newBuilder()
            if (actual == null) {
                // Am I observing an IR generation regression?
                // I see a lack of arg for an empty vararg,
                // rather than an empty vararg node.
                assert(it.varargElementType != null || it.hasDefaultValue())
            } else {
                argOrNull.expression = serializeExpression(actual)
            }
            proto.addValueArgument(argOrNull)
        }
        return proto.build()
    }

    private fun serializeCall(call: IrCall): KonanIr.IrCall {
        val proto = KonanIr.IrCall.newBuilder()

        proto.kind = irCallToPrimitiveKind(call)
        proto.descriptor = serializeDescriptor(call.descriptor)

        if (call.superQualifier != null) {
            proto.`super` = serializeDescriptor(call.superQualifier!!)
        }
        proto.memberAccess = serializeMemberAccessCommon(call)
        return proto.build()
    }

    private fun serializeCallableReference(callable: IrCallableReference): KonanIr.IrCallableReference {
        val proto = KonanIr.IrCallableReference.newBuilder()
            .setDescriptor(serializeDescriptor(callable.descriptor))
            .setTypeArguments(serializeTypeArguments(callable))

        return proto.build()
    }

    private fun serializeClassReference(expression: IrClassReference): KonanIr.IrClassReference {
        val proto = KonanIr.IrClassReference.newBuilder()
            .setClassDescriptor(serializeDescriptor(expression.symbol.descriptor))
        return proto.build()
    }

    private fun serializeConst(value: IrConst<*>): KonanIr.IrConst {
        val proto = KonanIr.IrConst.newBuilder()
        when (value.kind) {
            IrConstKind.Null        -> proto.`null` = true
            IrConstKind.Boolean     -> proto.boolean = value.value as Boolean
            IrConstKind.Byte        -> proto.byte = (value.value as Byte).toInt()
            IrConstKind.Char        -> proto.char = (value.value as Char).toInt()
            IrConstKind.Short       -> proto.short = (value.value as Short).toInt()
            IrConstKind.Int         -> proto.int = value.value as Int
            IrConstKind.Long        -> proto.long = value.value as Long
            IrConstKind.String      -> proto.string = value.value as String
            IrConstKind.Float       -> proto.float = value.value as Float
            IrConstKind.Double      -> proto.double = value.value as Double
            else -> {
                TODO("Const type serialization not implemented yet: ${ir2string(value)}")
            }
        }
        return proto.build()
    }

    private fun serializeDelegatingConstructorCall(call: IrDelegatingConstructorCall): KonanIr.IrDelegatingConstructorCall {
        val proto = KonanIr.IrDelegatingConstructorCall.newBuilder()
            .setDescriptor(serializeDescriptor(call.descriptor))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeDoWhile(expression: IrDoWhileLoop): KonanIr.IrDoWhile {
        val proto = KonanIr.IrDoWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    fun serializeEnumConstructorCall(call: IrEnumConstructorCall): KonanIr.IrEnumConstructorCall {
        val proto = KonanIr.IrEnumConstructorCall.newBuilder()
            .setDescriptor(serializeDescriptor(call.descriptor))
            .setMemberAccess(serializeMemberAccessCommon(call))
        return proto.build()
    }

    private fun serializeGetClass(expression: IrGetClass): KonanIr.IrGetClass {
        val proto = KonanIr.IrGetClass.newBuilder()
            .setArgument(serializeExpression(expression.argument))
        return proto.build()
    }

    private fun serializeGetEnumValue(expression: IrGetEnumValue): KonanIr.IrGetEnumValue {
        val proto = KonanIr.IrGetEnumValue.newBuilder()
            .setType(serializeKotlinType(expression.type))
            .setDescriptor(serializeDescriptor(expression.descriptor))
        return proto.build()
    }

    private fun serializeFieldAccessCommon(expression: IrFieldAccessExpression): KonanIr.FieldAccessCommon {
        val proto = KonanIr.FieldAccessCommon.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
        val superQualifier = expression.superQualifier
        if (superQualifier != null)
            proto.`super` = serializeDescriptor(superQualifier)
        val receiver = expression.receiver
        if (receiver != null)
            proto.receiver = serializeExpression(receiver)
        return proto.build()
    }

    private fun serializeGetField(expression: IrGetField): KonanIr.IrGetField {
        val proto = KonanIr.IrGetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
        return proto.build()
    }

    private fun serializeGetValue(expression: IrGetValue): KonanIr.IrGetValue {
        val proto = KonanIr.IrGetValue.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
        return proto.build()
    }

    private fun serializeGetObject(expression: IrGetObjectValue): KonanIr.IrGetObject {
        val proto = KonanIr.IrGetObject.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
        return proto.build()
    }

    private fun serializeInstanceInitializerCall(call: IrInstanceInitializerCall): KonanIr.IrInstanceInitializerCall {
        val proto = KonanIr.IrInstanceInitializerCall.newBuilder()

        proto.descriptor = serializeDescriptor(call.classDescriptor)

        return proto.build()
    }

    private fun serializeReturn(expression: IrReturn): KonanIr.IrReturn {
        val proto = KonanIr.IrReturn.newBuilder()
            .setReturnTarget(serializeDescriptor(expression.returnTarget))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetField(expression: IrSetField): KonanIr.IrSetField {
        val proto = KonanIr.IrSetField.newBuilder()
            .setFieldAccess(serializeFieldAccessCommon(expression))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSetVariable(expression: IrSetVariable): KonanIr.IrSetVariable {
        val proto = KonanIr.IrSetVariable.newBuilder()
            .setDescriptor(serializeDescriptor(expression.descriptor))
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeSpreadElement(element: IrSpreadElement): KonanIr.IrSpreadElement {
        val coordinates = serializeCoordinates(element.startOffset, element.endOffset)
        return KonanIr.IrSpreadElement.newBuilder()
            .setExpression(serializeExpression(element.expression))
            .setCoordinates(coordinates)
            .build()
    }

    private fun serializeThrow(expression: IrThrow): KonanIr.IrThrow {
        val proto = KonanIr.IrThrow.newBuilder()
            .setValue(serializeExpression(expression.value))
        return proto.build()
    }

    private fun serializeTry(expression: IrTry): KonanIr.IrTry {
        val proto = KonanIr.IrTry.newBuilder()
            .setResult(serializeExpression(expression.tryResult))
        val catchList = expression.catches
        catchList.forEach {
            proto.addCatch(serializeStatement(it))
        }
        val finallyExpression = expression.finallyExpression
        if (finallyExpression != null) {
            proto.finally = serializeExpression(finallyExpression)
        }
        return proto.build()
    }

    private fun serializeTypeOperator(operator: IrTypeOperator): KonanIr.IrTypeOperator = when (operator) {
        IrTypeOperator.CAST
            -> KonanIr.IrTypeOperator.CAST
        IrTypeOperator.IMPLICIT_CAST
            -> KonanIr.IrTypeOperator.IMPLICIT_CAST
        IrTypeOperator.IMPLICIT_NOTNULL
            -> KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            -> KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        IrTypeOperator.SAFE_CAST
            -> KonanIr.IrTypeOperator.SAFE_CAST
        IrTypeOperator.INSTANCEOF
            -> KonanIr.IrTypeOperator.INSTANCEOF
        IrTypeOperator.NOT_INSTANCEOF
            -> KonanIr.IrTypeOperator.NOT_INSTANCEOF
        else -> TODO("Unknown type operator")
    }

    private fun serializeTypeOp(expression: IrTypeOperatorCall): KonanIr.IrTypeOp {
        val proto = KonanIr.IrTypeOp.newBuilder()
            .setOperator(serializeTypeOperator(expression.operator))
            .setOperand(serializeKotlinType(expression.typeOperand))
            .setArgument(serializeExpression(expression.argument))
        return proto.build()

    }

    private fun serializeVararg(expression: IrVararg): KonanIr.IrVararg {
        val proto = KonanIr.IrVararg.newBuilder()
            .setElementType(serializeKotlinType(expression.varargElementType))
        expression.elements.forEach {
            proto.addElement(serializeVarargElement(it))
        }
        return proto.build()
    }

    private fun serializeVarargElement(element: IrVarargElement): KonanIr.IrVarargElement {
        val proto = KonanIr.IrVarargElement.newBuilder()
        when (element) {
            is IrExpression
                -> proto.expression = serializeExpression(element)
            is IrSpreadElement
                -> proto.spreadElement = serializeSpreadElement(element)
            else -> TODO("Unknown vararg element kind")
        }
        return proto.build()
    }

    private fun serializeWhen(expression: IrWhen): KonanIr.IrWhen {
        val proto = KonanIr.IrWhen.newBuilder()

        val branches = expression.branches
        branches.forEach {
            proto.addBranch(serializeStatement(it))
        }

        return proto.build()
    }

    private fun serializeLoop(expression: IrLoop): KonanIr.Loop {
        val proto = KonanIr.Loop.newBuilder()
            .setCondition(serializeExpression(expression.condition))
        val label = expression.label
        if (label != null) {
            proto.label = label
        }

        proto.loopId = currentLoopIndex
        loopIndex[expression] = currentLoopIndex++

        val body = expression.body
        if (body != null) {
            proto.body = serializeExpression(body)
        }

        return proto.build()
    }

    private fun serializeWhile(expression: IrWhileLoop): KonanIr.IrWhile {
        val proto = KonanIr.IrWhile.newBuilder()
            .setLoop(serializeLoop(expression))

        return proto.build()
    }

    private fun serializeBreak(expression: IrBreak): KonanIr.IrBreak {
        val proto = KonanIr.IrBreak.newBuilder()
        val label = expression.label
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeContinue(expression: IrContinue): KonanIr.IrContinue {
        val proto = KonanIr.IrContinue.newBuilder()
        val label = expression.label
        if (label != null) {
            proto.label = label
        }
        val loopId = loopIndex[expression.loop]!!
        proto.loopId = loopId

        return proto.build()
    }

    private fun serializeExpression(expression: IrExpression): KonanIr.IrExpression {
        context.log{"### serializing Expression: ${ir2string(expression)}"}

        val coordinates = serializeCoordinates(expression.startOffset, expression.endOffset)
        val proto = KonanIr.IrExpression.newBuilder()
            .setType(serializeKotlinType(expression.type))
            .setCoordinates(coordinates)

        val operationProto = KonanIr.IrOperation.newBuilder()
        
        when (expression) {
            is IrBlock       -> operationProto.block = serializeBlock(expression)
            is IrBreak       -> operationProto.`break` = serializeBreak(expression)
            is IrClassReference
                             -> operationProto.classReference = serializeClassReference(expression)
            is IrCall        -> operationProto.call = serializeCall(expression)
            is IrCallableReference
                             -> operationProto.callableReference = serializeCallableReference(expression)
            is IrComposite   -> operationProto.composite = serializeComposite(expression)
            is IrConst<*>    -> operationProto.const = serializeConst(expression)
            is IrContinue    -> operationProto.`continue` = serializeContinue(expression)
            is IrDelegatingConstructorCall
                             -> operationProto.delegatingConstructorCall = serializeDelegatingConstructorCall(expression)
            is IrDoWhileLoop -> operationProto.doWhile = serializeDoWhile(expression)
            is IrGetClass    -> operationProto.getClass = serializeGetClass(expression)
            is IrGetField    -> operationProto.getField = serializeGetField(expression)
            is IrGetValue    -> operationProto.getValue = serializeGetValue(expression)
            is IrGetEnumValue    
                             -> operationProto.getEnumValue = serializeGetEnumValue(expression)
            is IrGetObjectValue    
                             -> operationProto.getObject = serializeGetObject(expression)
            is IrInstanceInitializerCall        
                             -> operationProto.instanceInitializerCall = serializeInstanceInitializerCall(expression)
            is IrReturn      -> operationProto.`return` = serializeReturn(expression)
            is IrSetField    -> operationProto.setField = serializeSetField(expression)
            is IrSetVariable -> operationProto.setVariable = serializeSetVariable(expression)
            is IrStringConcatenation 
                             -> operationProto.stringConcat = serializeStringConcat(expression)
            is IrThrow       -> operationProto.`throw` = serializeThrow(expression)
            is IrTry         -> operationProto.`try` = serializeTry(expression)
            is IrTypeOperatorCall 
                             -> operationProto.typeOp = serializeTypeOp(expression)
            is IrVararg      -> operationProto.vararg = serializeVararg(expression)
            is IrWhen        -> operationProto.`when` = serializeWhen(expression)
            is IrWhileLoop   -> operationProto.`while` = serializeWhile(expression)
            else -> {
                TODO("Expression serialization not implemented yet: ${ir2string(expression)}.")
            }
        }
        proto.setOperation(operationProto)

        return proto.build()
    }

    private fun serializeStatement(statement: IrElement): KonanIr.IrStatement {
        context.log{"### serializing Statement: ${ir2string(statement)}"}

        val coordinates = serializeCoordinates(statement.startOffset, statement.endOffset)
        val proto = KonanIr.IrStatement.newBuilder()
            .setCoordinates(coordinates)

        when (statement) {
            is IrDeclaration -> proto.declaration = serializeDeclaration(statement)
            is IrExpression -> proto.expression = serializeExpression(statement)
            is IrBlockBody -> proto.blockBody = serializeBlockBody(statement)
            is IrBranch    -> proto.branch = serializeBranch(statement)
            is IrCatch    -> proto.catch = serializeCatch(statement)
            else -> {
                TODO("Statement not implemented yet: ${ir2string(statement)}")
            }
        }
        return proto.build()
    }

    private fun serializeIrFunction(function: IrFunction): KonanIr.IrFunction {
        val proto = KonanIr.IrFunction.newBuilder()
        val body = function.body
        if (body != null) proto.body = serializeStatement(body)

        function.descriptor.valueParameters.forEachIndexed { index, it ->
            val default = function.getDefault(it)
            if (default != null) {
                val pair = KonanIr.IrFunction.DefaultArgument.newBuilder()
                pair.position = index
                pair.value = serializeExpression(default.expression)
                proto.addDefaultArgument(pair)
            }
        }
        return proto.build()
    }

    private fun serializeIrProperty(property: IrProperty): KonanIr.IrProperty {
        val proto = KonanIr.IrProperty.newBuilder()
            .setIsDelegated(property.isDelegated)
        val backingField = property.backingField
        val getter = property.getter
        val setter = property.setter
        if (backingField != null)
            proto.backingField = serializeIrField(backingField)
        if (getter != null)
            proto.getter = serializeIrFunction(getter)
        if (setter != null)
            proto.setter = serializeIrFunction(setter)

        return proto.build()
    }

    private fun serializeIrField(field: IrField): KonanIr.IrProperty.IrField {
        val proto = KonanIr.IrProperty.IrField.newBuilder()
        val initializer = field.initializer?.expression
        if (initializer != null) {
            proto.initializer = serializeExpression(initializer)
        }
        return proto.build()
    }

    private fun serializeIrVariable(variable: IrVariable): KonanIr.IrVar {
        val proto = KonanIr.IrVar.newBuilder()
        val initializer = variable.initializer
        if (initializer != null) {
            proto.initializer = serializeExpression(initializer)
        }
        return proto.build()
    }

    private fun serializeIrClass(@Suppress("UNUSED_PARAMETER") clazz: IrClass): KonanIr.IrClass {
        val proto = KonanIr.IrClass.newBuilder()

        // TODO: As of now we get here only for anonymous local objects.
        // There is still some work needed to support them.
        // Until it is done, let's pretend all IrClasses are empty.
        // So that we don't have to deal with their type tables.
        /*
        val declarations = clazz.declarations
        declarations.forEach {
            val descriptor = it.descriptor
            if (descriptor !is CallableMemberDescriptor || descriptor.kind.isReal) {
                proto.addMember(serializeDeclaration(it))
            }
        }
        */
        return proto.build()
    }

    private fun serializeIrEnumEntry(enumEntry: IrEnumEntry): KonanIr.IrEnumEntry {
        val proto = KonanIr.IrEnumEntry.newBuilder()
        val initializer = enumEntry.initializerExpression!!
        proto.initializer = serializeExpression(initializer)
        val correspondingClass = enumEntry.correspondingClass
        if (correspondingClass != null) {
            proto.correspondingClass = serializeDeclaration(correspondingClass)
        }
        return proto.build()
    }

    private fun serializeDeclaration(declaration: IrDeclaration): KonanIr.IrDeclaration {
        context.log{"### serializing Declaration: ${ir2string(declaration)}"}

        val descriptor = declaration.descriptor

        if (descriptor != rootFunction &&
                declaration !is IrVariable) {
            localDeclarationSerializer.pushContext(descriptor)
        }

        var kotlinDescriptor = serializeDescriptor(descriptor)
        var realDescriptor: KonanIr.DeclarationDescriptor? = null
        if (descriptor != rootFunction) {
            realDescriptor = localDeclarationSerializer.serializeLocalDeclaration(descriptor)
        }
        val declarator = KonanIr.IrDeclarator.newBuilder()

        when (declaration) {
            is IrFunction 
                -> declarator.function = serializeIrFunction(declaration)
            is IrVariable 
                -> declarator.variable = serializeIrVariable(declaration)
            is IrClass
                -> declarator.irClass = serializeIrClass(declaration)
            is IrEnumEntry
                -> declarator.irEnumEntry = serializeIrEnumEntry(declaration)
            is IrProperty
                -> declarator.irProperty = serializeIrProperty(declaration)
            else 
                -> {
                TODO("Declaration serialization not supported yet: $declaration")
            }
        }

        if (declaration !is IrVariable) {
            localDeclarationSerializer.popContext()
        }

        if (descriptor != rootFunction) {
            val localDeclaration = KonanIr.LocalDeclaration
                .newBuilder()
                .setDescriptor(realDescriptor!!)
                .build()
            kotlinDescriptor = kotlinDescriptor
                .toBuilder()
                .setIrLocalDeclaration(localDeclaration)
                .build()
        }

        val coordinates = serializeCoordinates(declaration.startOffset, declaration.endOffset)
        val proto = KonanIr.IrDeclaration.newBuilder()
            //.setKind(declaration.irKind())
            .setDescriptor(kotlinDescriptor)
            .setCoordinates(coordinates)


        proto.setDeclarator(declarator)
        val fileName = context.ir.originalModuleIndex.declarationToFile[declaration.descriptor]
        proto.fileName = fileName

        return proto.build()
    }

    private fun encodeDeclaration(declaration: IrDeclaration): String {
        val proto = serializeDeclaration(declaration)
        val byteArray = proto.toByteArray()
        return base64Encode(byteArray)
    }


}

// --------- Deserializer part -----------------------------

internal class IrDeserializer(val context: Context,
                              private val rootFunction: FunctionDescriptor) {

    private val loopIndex = mutableMapOf<Int, IrLoop>()

    private val rootMember = rootFunction.deserializedPropertyIfAccessor
    private val localDeserializer = LocalDeclarationDeserializer(rootMember)

    private val descriptorDeserializer = IrDescriptorDeserializer(
        context, rootMember, localDeserializer)

    private fun deserializeKotlinType(proto: KonanIr.KotlinType)
        = descriptorDeserializer.deserializeKotlinType(proto)

    private fun deserializeDescriptor(proto: KonanIr.KotlinDescriptor)
        = descriptorDeserializer.deserializeDescriptor(proto)

    private fun deserializeTypeArguments(proto: KonanIr.TypeArguments): List<IrType> {
        context.log{"### deserializeTypeArguments"}
        val result = mutableListOf<IrType>()
        proto.typeArgumentList.forEach { type ->
            val kotlinType = deserializeKotlinType(type)
            result.add(kotlinType.brokenIr)
            context.log{"$kotlinType"}
        }
        return result
    }

    private fun deserializeBlockBody(proto: KonanIr.IrBlockBody,
                                     start: Int, end: Int): IrBlockBody {

        val statements = mutableListOf<IrStatement>()

        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockBodyImpl(start, end, statements)
    }

    private fun deserializeBranch(proto: KonanIr.IrBranch, start: Int, end: Int): IrBranch {

        val condition = deserializeExpression(proto.condition)
        val result = deserializeExpression(proto.result)

        return IrBranchImpl(start, end, condition, result)
    }

    private fun deserializeCatch(proto: KonanIr.IrCatch, start: Int, end: Int): IrCatch {
        val catchParameter = deserializeDeclaration(proto.catchParameter) as IrVariable
        val result = deserializeExpression(proto.result)

        return IrCatchImpl(start, end, catchParameter, result)
    }

    private fun deserializeStatement(proto: KonanIr.IrStatement): IrElement {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val element = when {
            proto.hasBlockBody() 
                -> deserializeBlockBody(proto.blockBody, start, end)
            proto.hasBranch()
                -> deserializeBranch(proto.branch, start, end)
            proto.hasCatch()
                -> deserializeCatch(proto.catch, start, end)
            proto.hasDeclaration()
                -> deserializeDeclaration(proto.declaration)
            proto.hasExpression()
                -> deserializeExpression(proto.expression)
            else -> {
                TODO("Statement deserialization not implemented")
            }
        }

        context.log{"### Deserialized statement: ${ir2string(element)}"}

        return element
    }

    private val KotlinType.ir: IrType get() = context.ir.translateErased(this)
    private val KotlinType.brokenIr: IrType get() = context.ir.translateBroken(this)

    private fun deserializeBlock(proto: KonanIr.IrBlock, start: Int, end: Int, type: KotlinType): IrBlock {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        val isLambdaOrigin = if (proto.isLambdaOrigin) IrStatementOrigin.LAMBDA else null

        return IrBlockImpl(start, end, type.ir, isLambdaOrigin, statements)
    }

    private fun deserializeMemberAccessCommon(access: IrMemberAccessExpression, proto: KonanIr.MemberAccessCommon) {

        proto.valueArgumentList.mapIndexed { i, arg ->
            val exprOrNull = if (arg.hasExpression())
                deserializeExpression(arg.expression)
            else null
            access.putValueArgument(i, exprOrNull)
        }

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, type ->
            access.putTypeArgument(index, type)
        }

        if (proto.hasDispatchReceiver()) {
            access.dispatchReceiver = deserializeExpression(proto.dispatchReceiver)
        }
        if (proto.hasExtensionReceiver()) {
            access.extensionReceiver = deserializeExpression(proto.extensionReceiver)
        }
    }

    private fun deserializeClassReference(proto: KonanIr.IrClassReference, start: Int, end: Int, type: KotlinType): IrClassReference {
        val descriptor = deserializeDescriptor(proto.classDescriptor) as ClassifierDescriptor
        /** TODO: [createClassifierSymbolForClassReference] is internal function */
        @Suppress("DEPRECATION")
        return IrClassReferenceImpl(start, end, type.ir, descriptor, descriptor.defaultType.ir)
    }

    private fun deserializeCall(proto: KonanIr.IrCall, start: Int, end: Int, type: KotlinType): IrCall {
        val descriptor = deserializeDescriptor(proto.descriptor) as FunctionDescriptor

        val superDescriptor = if (proto.hasSuper()) {
            deserializeDescriptor(proto.`super`) as ClassDescriptor
        } else null

        val call: IrCall = when (proto.kind) {
            KonanIr.IrCall.Primitive.NOT_PRIMITIVE ->
                // TODO: implement the last three args here.
                IrCallImpl(start, end, type.ir, createFunctionSymbol(descriptor), descriptor, proto.memberAccess.typeArguments.typeArgumentCount, null, createClassSymbolOrNull(superDescriptor))
            KonanIr.IrCall.Primitive.NULLARY ->
                IrNullaryPrimitiveImpl(start, end, type.ir, null, createFunctionSymbol(descriptor))
            KonanIr.IrCall.Primitive.UNARY ->
                IrUnaryPrimitiveImpl(start, end, type.ir, null, createFunctionSymbol(descriptor))
            KonanIr.IrCall.Primitive.BINARY ->
                IrBinaryPrimitiveImpl(start, end, type.ir, null, createFunctionSymbol(descriptor))
            else -> TODO("Unexpected primitive IrCall.")
        }
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeCallableReference(proto: KonanIr.IrCallableReference,
                                             start: Int, end: Int, type: KotlinType): IrCallableReference {

        val descriptor = deserializeDescriptor(proto.descriptor) as CallableDescriptor
        val callable = when (descriptor) {
            is FunctionDescriptor -> IrFunctionReferenceImpl(start, end, type.ir, createFunctionSymbol(descriptor), descriptor, proto.typeArguments.typeArgumentCount, null)
            else -> TODO()
        }

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, argType ->
            callable.putTypeArgument(index, argType)
        }
        return callable
    }

    private fun deserializeComposite(proto: KonanIr.IrComposite, start: Int, end: Int, type: KotlinType): IrComposite {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }
        return IrCompositeImpl(start, end, type.ir, null, statements)
    }

    private fun deserializeDelegatingConstructorCall(proto: KonanIr.IrDelegatingConstructorCall, start: Int, end: Int): IrDelegatingConstructorCall {
        val descriptor = deserializeDescriptor(proto.descriptor) as ClassConstructorDescriptor
        val call = IrDelegatingConstructorCallImpl(start, end, context.irBuiltIns.unitType, IrConstructorSymbolImpl(descriptor.original), descriptor, proto.memberAccess.typeArguments.typeArgumentCount)

        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeGetClass(proto: KonanIr.IrGetClass, start: Int, end: Int, type: KotlinType): IrGetClass {
        val argument = deserializeExpression(proto.argument)
        return IrGetClassImpl(start, end, type.ir, argument)
    }

    private fun deserializeGetField(proto: KonanIr.IrGetField, start: Int, end: Int): IrGetField {
        val access = proto.fieldAccess
        val descriptor = deserializeDescriptor(access.descriptor) as PropertyDescriptor
        val superQualifier = if (access.hasSuper()) {
            deserializeDescriptor(access.descriptor) as ClassDescriptor
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null

        return IrGetFieldImpl(start, end, IrFieldSymbolImpl(descriptor), descriptor.type.ir, receiver, null, createClassSymbolOrNull(superQualifier))
    }

    private fun deserializeGetValue(proto: KonanIr.IrGetValue, start: Int, end: Int): IrGetValue {
        val descriptor = deserializeDescriptor(proto.descriptor) as ValueDescriptor

        // TODO: origin!
        return IrGetValueImpl(start, end, descriptor.type.ir, createValueSymbol(descriptor), null)
    }

    private fun deserializeGetEnumValue(proto: KonanIr.IrGetEnumValue, start: Int, end: Int): IrGetEnumValue {
        val type = deserializeKotlinType(proto.type)
        val descriptor = deserializeDescriptor(proto.descriptor) as ClassDescriptor

        return IrGetEnumValueImpl(start, end, type.ir, IrEnumEntrySymbolImpl(descriptor))
    }

    private fun deserializeGetObject(proto: KonanIr.IrGetObject, start: Int, end: Int, type: KotlinType): IrGetObjectValue {
        val descriptor = deserializeDescriptor(proto.descriptor) as ClassDescriptor
        return IrGetObjectValueImpl(start, end, type.ir, IrClassSymbolImpl(descriptor))
    }

    private fun deserializeInstanceInitializerCall(proto: KonanIr.IrInstanceInitializerCall, start: Int, end: Int): IrInstanceInitializerCall {
        val descriptor = deserializeDescriptor(proto.descriptor) as ClassDescriptor

        return IrInstanceInitializerCallImpl(start, end, IrClassSymbolImpl(descriptor), context.irBuiltIns.unitType)
    }

    private fun deserializeReturn(proto: KonanIr.IrReturn, start: Int, end: Int, type: KotlinType): IrReturn {
        val descriptor = 
            deserializeDescriptor(proto.returnTarget) as FunctionDescriptor
        val value = deserializeExpression(proto.value)
        return IrReturnImpl(start, end, context.irBuiltIns.nothingType, createFunctionSymbol(descriptor), value)
    }

    private fun deserializeSetField(proto: KonanIr.IrSetField, start: Int, end: Int): IrSetField {
        val access = proto.fieldAccess
        val descriptor = deserializeDescriptor(access.descriptor) as PropertyDescriptor
        val superQualifier = if (access.hasSuper()) {
            deserializeDescriptor(access.descriptor) as ClassDescriptor
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null
        val value = deserializeExpression(proto.value)

        return IrSetFieldImpl(start, end, IrFieldSymbolImpl(descriptor), receiver, value, context.irBuiltIns.unitType, null, createClassSymbolOrNull(superQualifier))
    }

    private fun deserializeSetVariable(proto: KonanIr.IrSetVariable, start: Int, end: Int): IrSetVariable {
        val descriptor = deserializeDescriptor(proto.descriptor) as VariableDescriptor
        val value = deserializeExpression(proto.value)
        return IrSetVariableImpl(start, end, context.irBuiltIns.unitType, IrVariableSymbolImpl(descriptor), value, null)
    }

    private fun deserializeSpreadElement(proto: KonanIr.IrSpreadElement): IrSpreadElement {
        val expression = deserializeExpression(proto.expression)
        return IrSpreadElementImpl(proto.coordinates.startOffset, proto.coordinates.endOffset, expression)
    }

    private fun deserializeStringConcat(proto: KonanIr.IrStringConcat, start: Int, end: Int, type: KotlinType): IrStringConcatenation {
        val argumentProtos = proto.argumentList
        val arguments = mutableListOf<IrExpression>()

        argumentProtos.forEach {
            arguments.add(deserializeExpression(it))
        }
        return IrStringConcatenationImpl(start, end, type.ir, arguments)
    }

    private fun deserializeThrow(proto: KonanIr.IrThrow, start: Int, end: Int, type: KotlinType): IrThrowImpl {
        return IrThrowImpl(start, end, context.irBuiltIns.nothingType, deserializeExpression(proto.value))
    }

    private fun deserializeTry(proto: KonanIr.IrTry, start: Int, end: Int, type: KotlinType): IrTryImpl {
        val result = deserializeExpression(proto.result)
        val catches = mutableListOf<IrCatch>()
        proto.catchList.forEach {
            catches.add(deserializeStatement(it) as IrCatch) 
        }
        val finallyExpression = 
            if (proto.hasFinally()) deserializeExpression(proto.getFinally()) else null
        return IrTryImpl(start, end, type.ir, result, catches, finallyExpression)
    }

    private fun deserializeTypeOperator(operator: KonanIr.IrTypeOperator): IrTypeOperator {
        when (operator) {
            KonanIr.IrTypeOperator.CAST
                -> return IrTypeOperator.CAST
            KonanIr.IrTypeOperator.IMPLICIT_CAST
                -> return IrTypeOperator.IMPLICIT_CAST
            KonanIr.IrTypeOperator.IMPLICIT_NOTNULL
                -> return IrTypeOperator.IMPLICIT_NOTNULL
            KonanIr.IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
                -> return IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
            KonanIr.IrTypeOperator.SAFE_CAST
                -> return IrTypeOperator.SAFE_CAST
            KonanIr.IrTypeOperator.INSTANCEOF
                -> return IrTypeOperator.INSTANCEOF
            KonanIr.IrTypeOperator.NOT_INSTANCEOF
                -> return IrTypeOperator.NOT_INSTANCEOF
            else -> TODO("Unknown type operator")
        }
    }

    private fun deserializeTypeOp(proto: KonanIr.IrTypeOp, start: Int, end: Int, type: KotlinType) : IrTypeOperatorCall {
        val operator = deserializeTypeOperator(proto.operator)
        val operand = deserializeKotlinType(proto.operand).brokenIr
        val argument = deserializeExpression(proto.argument)
        return IrTypeOperatorCallImpl(start, end, type.ir, operator, operand).apply {
            this.argument = argument
            this.typeOperandClassifier = operand.classifierOrFail
        }
    }

    private fun deserializeVararg(proto: KonanIr.IrVararg, start: Int, end: Int, type: KotlinType): IrVararg {
        val elementType = deserializeKotlinType(proto.elementType)

        val elements = mutableListOf<IrVarargElement>()
        proto.elementList.forEach {
            elements.add(deserializeVarargElement(it))
        }
        return IrVarargImpl(start, end, type.ir, elementType.ir, elements)
    }

    private fun deserializeVarargElement(element: KonanIr.IrVarargElement): IrVarargElement {
        return when (element.varargElementCase) {
            EXPRESSION 
                -> deserializeExpression(element.expression)
            SPREAD_ELEMENT 
                -> deserializeSpreadElement(element.spreadElement)
            else 
                -> TODO("Unexpected vararg element")
        }
    }

    private fun deserializeWhen(proto: KonanIr.IrWhen, start: Int, end: Int, type: KotlinType): IrWhen {
        val branches = mutableListOf<IrBranch>()

        proto.branchList.forEach {
            branches.add(deserializeStatement(it) as IrBranch)
        }

        // TODO: provide some origin!
        return  IrWhenImpl(start, end, type.ir, null, branches)
    }

    private fun deserializeLoop(proto: KonanIr.Loop, loop: IrLoopBase): IrLoopBase {
        val loopId = proto.loopId
        loopIndex.getOrPut(loopId){loop}

        val label = if (proto.hasLabel()) proto.label else null
        val body = if (proto.hasBody()) deserializeExpression(proto.body) else null
        val condition = deserializeExpression(proto.condition)

        loop.label = label
        loop.condition = condition
        loop.body = body

        return loop
    }

    private fun deserializeDoWhile(proto: KonanIr.IrDoWhile, start: Int, end: Int, type: KotlinType): IrDoWhileLoop {
        // we create the loop before deserializing the body, so that 
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrDoWhileLoopImpl(start, end, type.ir, null)
        deserializeLoop(proto.loop, loop)
        return loop
    }

    private fun deserializeWhile(proto: KonanIr.IrWhile, start: Int, end: Int, type: KotlinType): IrWhileLoop {
        // we create the loop before deserializing the body, so that 
        // IrBreak statements have something to put into 'loop' field.
        val loop = IrWhileLoopImpl(start, end, type.ir, null)
        deserializeLoop(proto.loop, loop)
        return loop
    }

    private fun deserializeBreak(proto: KonanIr.IrBreak, start: Int, end: Int, type: KotlinType): IrBreak {
        val label = if(proto.hasLabel()) proto.label else null
        val loopId = proto.loopId
        val loop = loopIndex[loopId]!!
        val irBreak = IrBreakImpl(start, end, type.ir, loop)
        irBreak.label = label

        return irBreak
    }

    private fun deserializeContinue(proto: KonanIr.IrContinue, start: Int, end: Int, type: KotlinType): IrContinue {
        val label = if(proto.hasLabel()) proto.label else null
        val loopId = proto.loopId
        val loop = loopIndex[loopId]!!
        val irContinue = IrContinueImpl(start, end, type.ir, loop)
        irContinue.label = label

        return irContinue
    }

    private fun deserializeConst(proto: KonanIr.IrConst, start: Int, end: Int, type: KotlinType): IrExpression =
        when(proto.valueCase) {
            NULL
                -> IrConstImpl.constNull(start, end, type.ir)
            BOOLEAN
                -> IrConstImpl.boolean(start, end, type.ir, proto.boolean)
            BYTE
                -> IrConstImpl.byte(start, end, type.ir, proto.byte.toByte())
            CHAR
                -> IrConstImpl.char(start, end, type.ir, proto.char.toChar())
            SHORT
                -> IrConstImpl.short(start, end, type.ir, proto.short.toShort())
            INT
                -> IrConstImpl.int(start, end, type.ir, proto.int)
            LONG
                -> IrConstImpl.long(start, end, type.ir, proto.long)
            STRING
                -> IrConstImpl.string(start, end, type.ir, proto.string)
            FLOAT
                -> IrConstImpl.float(start, end, type.ir, proto.float)
            DOUBLE
                -> IrConstImpl.double(start, end, type.ir, proto.double)
            else -> {
                TODO("Not all const types have been implemented")
            }
        }

    private fun deserializeOperation(proto: KonanIr.IrOperation, start: Int, end: Int, type: KotlinType): IrExpression =
        when (proto.operationCase) {
            BLOCK
                -> deserializeBlock(proto.block, start, end, type)
            BREAK
                -> deserializeBreak(proto.`break`, start, end, type)
            CLASS_REFERENCE
                -> deserializeClassReference(proto.classReference, start, end, type)
            CALL
                -> deserializeCall(proto.call, start, end, type)
            CALLABLE_REFERENCE
                -> deserializeCallableReference(proto.callableReference, start, end, type)
            COMPOSITE
                -> deserializeComposite(proto.composite, start, end, type)
            CONST
                -> deserializeConst(proto.const, start, end, type)
            CONTINUE
                -> deserializeContinue(proto.`continue`, start, end, type)
            DELEGATING_CONSTRUCTOR_CALL
                -> deserializeDelegatingConstructorCall(proto.delegatingConstructorCall, start, end)
            DO_WHILE
                -> deserializeDoWhile(proto.doWhile, start, end, type)
            GET_ENUM_VALUE
                -> deserializeGetEnumValue(proto.getEnumValue, start, end)
            GET_CLASS
                -> deserializeGetClass(proto.getClass, start, end, type)
            GET_FIELD
                -> deserializeGetField(proto.getField, start, end)
            GET_OBJECT
                -> deserializeGetObject(proto.getObject, start, end, type)
            GET_VALUE
                -> deserializeGetValue(proto.getValue, start, end)
            INSTANCE_INITIALIZER_CALL
                -> deserializeInstanceInitializerCall(proto.instanceInitializerCall, start, end)
            RETURN
                -> deserializeReturn(proto.`return`, start, end, type)
            SET_FIELD
                -> deserializeSetField(proto.setField, start, end)
            SET_VARIABLE
                -> deserializeSetVariable(proto.setVariable, start, end)
            STRING_CONCAT
                -> deserializeStringConcat(proto.stringConcat, start, end, type)
            THROW
                -> deserializeThrow(proto.`throw`, start, end, type)
            TRY
                -> deserializeTry(proto.`try`, start, end, type)
            TYPE_OP
                -> deserializeTypeOp(proto.typeOp, start, end, type)
            VARARG
                -> deserializeVararg(proto.vararg, start, end, type)
            WHEN
                -> deserializeWhen(proto.`when`, start, end, type)
            WHILE
                -> deserializeWhile(proto.`while`, start, end, type)
            else -> {
                TODO("Expression deserialization not implemented: ${proto.operationCase}")
            }
        }

    private fun deserializeExpression(proto: KonanIr.IrExpression): IrExpression {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val type = deserializeKotlinType(proto.type)
        val operation = proto.operation
        val expression = deserializeOperation(operation, start, end, type)

        context.log{"### Deserialized expression: ${ir2string(expression)}"}
        return expression
    }

    private fun deserializeIrClass(proto: KonanIr.IrClass, descriptor: ClassDescriptor, start: Int, end: Int, origin: IrDeclarationOrigin): IrClass {
        val members = mutableListOf<IrDeclaration>()

        proto.memberList.forEach {
            members.add(deserializeDeclaration(it))
        }

        val clazz = IrClassImpl(start, end, origin, descriptor, members)

        val symbolTable = context.ir.symbols.symbolTable
        clazz.createParameterDeclarations(symbolTable)
        clazz.addFakeOverrides(symbolTable)
        clazz.setSuperSymbols(symbolTable)

        return clazz

    }

    private fun deserializeIrFunction(proto: KonanIr.IrFunction, descriptor: FunctionDescriptor,
                                      start: Int, end: Int, origin: IrDeclarationOrigin): IrFunction {

        val body = deserializeStatement(proto.body)
        val function = IrFunctionImpl(start, end, origin, descriptor)

        function.returnType = descriptor.returnType!!.ir
        function.body = body as IrBody

        function.createParameterDeclarations(context.ir.symbols.symbolTable)
        function.setOverrides(context.ir.symbols.symbolTable)

        proto.defaultArgumentList.forEach {
            val expr = deserializeExpression(it.value)

            function.putDefault(
                    descriptor.valueParameters[it.position],
                    IrExpressionBodyImpl(start, end, expr))
        }
        return function
    }

    private fun deserializeIrVariable(proto: KonanIr.IrVar, descriptor: VariableDescriptor,
                                      start: Int, end: Int, origin: IrDeclarationOrigin): IrVariable {

        val initializer = if (proto.hasInitializer()) {
            deserializeExpression(proto.initializer)
        } else null

        return IrVariableImpl(start, end, origin, descriptor, descriptor.type.ir, initializer)
    }

    private fun deserializeIrEnumEntry(proto: KonanIr.IrEnumEntry, descriptor: ClassDescriptor,
                                       start: Int, end: Int, origin: IrDeclarationOrigin): IrEnumEntry {

        val enumEntry = IrEnumEntryImpl(start, end, origin, descriptor)
        if (proto.hasCorrespondingClass()) {
            enumEntry.correspondingClass = deserializeDeclaration(proto.correspondingClass) as IrClass
        }
        enumEntry.initializerExpression = deserializeExpression(proto.initializer)

        return enumEntry
    }

    private fun deserializeDeclaration(proto: KonanIr.IrDeclaration): IrDeclaration {

        val descriptor = deserializeDescriptor(proto.descriptor)

        if (descriptor !is VariableDescriptor && descriptor != rootFunction)
            localDeserializer.pushContext(descriptor)

        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val origin = DEFINED // TODO: retore the real origins
        val declarator = proto.declarator

        val declaration: IrDeclaration = when {
            declarator.hasIrClass()
                -> deserializeIrClass(declarator.irClass,
                    descriptor as ClassDescriptor, start, end, origin)
            declarator.hasFunction() 
                -> deserializeIrFunction(declarator.function, 
                    descriptor as FunctionDescriptor, start, end, origin)
            declarator.hasVariable()
                -> deserializeIrVariable(declarator.variable, 
                    descriptor as VariableDescriptor, start, end, origin)
            declarator.hasIrEnumEntry()
                -> deserializeIrEnumEntry(declarator.irEnumEntry, 
                    descriptor as ClassDescriptor, start, end, origin)
            else -> {
                TODO("Declaration deserialization not implemented")
            }
        }

        val sourceFileName = proto.fileName
        context.ir.originalModuleIndex.declarationToFile[declaration.descriptor.original] = sourceFileName

        if (!(descriptor is VariableDescriptor) && descriptor != rootFunction)
            localDeserializer.popContext(descriptor)
        context.log{"### Deserialized declaration: ${ir2string(declaration)}"}
        return declaration
    }

    // We run inline body deserializations after the public descriptor tree
    // deserialization is long gone. So we don't have the needed chain of
    // deserialization contexts available to take type parameters.
    // So typeDeserializer introduces a brand new set of DeserializadTypeParameterDescriptors
    // for the rootFunction.
    // This function takes the type parameters from the rootFunction descriptor
    // and substitutes them instead the deserialized ones.
    // TODO: consider lazy inline body deserialization during the public descriptors deserialization.
    // I tried to copy over TypeDeserializaer, MemberDeserializer, 
    // and the rest of what's needed, but it didn't work out.
    private fun adaptDeserializedTypeParameters(declaration: IrDeclaration): IrDeclaration {

        val rootFunctionTypeParameters = 
            localDeserializer.typeDeserializer.ownTypeParameters

        val realTypeParameters =
            rootFunction.deserializedPropertyIfAccessor.typeParameters

        val substitutionContext = rootFunctionTypeParameters.mapIndexed{
            index, param ->
            Pair(param.typeConstructor, TypeProjectionImpl(realTypeParameters[index].defaultType))
        }.associate{
            (key,value) ->
        key to value}

        return DeepCopyIrTreeWithDescriptors(rootFunction, rootFunction.parents.first(), context).copy(
            irElement       = declaration,
            typeSubstitutor = TypeSubstitutor.create(substitutionContext)
        ) as IrFunction
    }

    private val extractInlineProto: KonanLinkData.InlineIrBody
        get() = when (rootFunction) {
            is DeserializedSimpleFunctionDescriptor -> {
                rootFunction.proto.inlineIr
            }
            is DeserializedClassConstructorDescriptor -> {
                rootFunction.proto.constructorIr
            }
            is PropertyGetterDescriptor -> {
                (rootMember as DeserializedPropertyDescriptor).proto.getterIr
            }
            is PropertySetterDescriptor -> {
                (rootMember as DeserializedPropertyDescriptor).proto.setterIr
            }
            else -> error("Unexpected descriptor: $rootFunction")
        }

    fun decodeDeclaration(): IrDeclaration {
        assert(rootFunction.isDeserializableCallable)

        val inlineProto = extractInlineProto
        val base64 = inlineProto.encodedIr
        val byteArray = base64Decode(base64)
        val irProto = KonanIr.IrDeclaration.parseFrom(byteArray, KonanSerializerProtocol.extensionRegistry)
        val declaration = deserializeDeclaration(irProto)

        return adaptDeserializedTypeParameters(declaration)
    }
}
