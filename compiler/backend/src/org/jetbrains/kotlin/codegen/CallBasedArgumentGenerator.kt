/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.org.objectweb.asm.Type

class CallBasedArgumentGenerator(
    private val codegen: ExpressionCodegen,
    private val callGenerator: CallGenerator,
    private val valueParameters: List<ValueParameterDescriptor>,
    private val valueParameterTypes: List<Type>
) : ArgumentGenerator() {
    private val isVarargInvoke: Boolean =
        JvmCodegenUtil.isDeclarationOfBigArityFunctionInvoke(valueParameters.firstOrNull()?.containingDeclaration)

    init {
        if (!isVarargInvoke) {
            assert(valueParameters.size == valueParameterTypes.size) {
                "Value parameters and their types mismatch in sizes: ${valueParameters.size} != ${valueParameterTypes.size}"
            }
        }
    }

    override fun generateExpression(i: Int, argument: ExpressionValueArgument) {
        val parameter = valueParameters[i]
        val valueArgument = argument.valueArgument!!
        val argumentExpression = valueArgument.getArgumentExpression() ?: error(valueArgument.asElement().text)
        callGenerator.genValueAndPut(parameter, argumentExpression, if (isVarargInvoke) OBJECT_TYPE else valueParameterTypes[i], i)
    }

    override fun generateDefault(i: Int, argument: DefaultValueArgument) {
        callGenerator.putValueIfNeeded(
            getJvmKotlinType(i),
            StackValue.createDefaultValue(valueParameterTypes[i]),
            ValueKind.DEFAULT_PARAMETER,
            i
        )
    }

    override fun generateVararg(i: Int, argument: VarargValueArgument) {
        // Upper bound for type of vararg parameter should always have a form of 'Array<out T>',
        // while its lower bound may be Nothing-typed after approximation
        val lazyVararg = codegen.genVarargs(argument, valueParameters[i].type.upperIfFlexible())
        callGenerator.putValueIfNeeded(getJvmKotlinType(i), lazyVararg, ValueKind.GENERAL_VARARG, i)
    }

    override fun generateDefaultJava(i: Int, argument: DefaultValueArgument) {
        val argumentValue = valueParameters[i].findJavaDefaultArgumentValue(valueParameterTypes[i], codegen.typeMapper)

        callGenerator.putValueIfNeeded(getJvmKotlinType(i), argumentValue)
    }

    override fun reorderArgumentsIfNeeded(args: List<ArgumentAndDeclIndex>) {
        callGenerator.reorderArgumentsIfNeeded(args, valueParameterTypes)
    }

    private fun getJvmKotlinType(i: Int): JvmKotlinType =
        JvmKotlinType(valueParameterTypes[i], valueParameters[i].original.type)
}
