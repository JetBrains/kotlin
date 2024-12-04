/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.IrCallGenerator
import org.jetbrains.kotlin.backend.jvm.codegen.MaterialValue
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.mapping.mapType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

object DataCopy : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        // do not change the definition of the `copy$default` method
        if (codegen.irFunction.name.asString() == "copy\$default" && codegen.irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) return null

        val owner = expression.symbol.owner

        fun IrValueParameter.asmType(): Type = codegen.typeMapper.mapType(this)

        val receiverParameter = owner.parameters.first()
        val receiverArgument = expression.arguments.first()!!
        val receiverAndReturnType = receiverParameter.asmType()

        val chosenValueParameters = mutableListOf<IrValueParameter>()
        val chosenValueArguments = mutableListOf<IrExpression>()

        when (owner.origin) {
            IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> owner.parameters.drop(1).dropLast(2)
            else -> owner.parameters.drop(1)
        }.forEachIndexed visitor@{ ix, parameter ->
            val argument = expression.arguments[ix + 1]
            if (argument == null || argument.isDefaultValue) return@visitor
            chosenValueParameters += parameter
            chosenValueArguments += argument
        }

        val generator = IrCallGenerator.DefaultCallGenerator
        generator.genValueAndPut(receiverParameter, receiverArgument, receiverParameter.asmType(), codegen, data)
        for (i in chosenValueParameters.indices) {
            val valueParameter = chosenValueParameters[i]
            val valueArgument = chosenValueArguments[i]
            generator.genValueAndPut(valueParameter, valueArgument, valueParameter.asmType(), codegen, data)
        }

        val descriptor = Type.getMethodDescriptor(
            receiverAndReturnType,
            receiverAndReturnType,
            *chosenValueParameters.map { it.asmType() }.toTypedArray()
        )
        codegen.mv.invokedynamic(
            "copy",
            descriptor,
            boostrapHandle,
            arrayOf(receiverAndReturnType, *chosenValueParameters.map { it.indexInParameters }.toTypedArray<Any>())
        )

        return MaterialValue(codegen, receiverAndReturnType, expression.type)
    }

    val boostrapHandle = Handle(
        Opcodes.H_INVOKESTATIC,
        "kotlin/internal/DataCopyBootstrap",
        "bootstrap",
        "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class<*>;[I)Ljava/lang/invoke/CallSite;",
        false
    )

    val IrExpression.isDefaultValue: Boolean
        get() = (this is IrGetValue && this.origin == IrStatementOrigin.DEFAULT_VALUE)
                || (this is IrComposite && this.origin == IrStatementOrigin.DEFAULT_VALUE)
                || (this is IrTypeOperatorCall && this.argument.isDefaultValue)
}