/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.interpreter.intrinsicConstEvaluationAnnotation
import org.jetbrains.kotlin.ir.interpreter.isConst
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnsignedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

enum class EvaluationMode {
    FULL {
        override fun canEvaluateFunction(function: IrFunction): Boolean = true
        override fun canEvaluateEnumValue(enumEntry: IrGetEnumValue): Boolean = true
        override fun canEvaluateFunctionExpression(expression: IrFunctionExpression): Boolean = true
        override fun canEvaluateCallableReference(reference: IrCallableReference<*>): Boolean = true
        override fun canEvaluateClassReference(reference: IrDeclarationReference): Boolean = true

        override fun canEvaluateBlock(block: IrBlock): Boolean = true
        override fun canEvaluateComposite(composite: IrComposite): Boolean = true

        override fun canEvaluateExpression(expression: IrExpression): Boolean = true

        override fun mustCheckBodyOf(function: IrFunction): Boolean = true
    },

    ONLY_BUILTINS {
        private val allowedMethodsOnPrimitives = setOf(
            "not", "unaryMinus", "unaryPlus", "inv",
            "toString", "toChar", "toByte", "toShort", "toInt", "toLong", "toFloat", "toDouble",
            "equals", "compareTo", "plus", "minus", "times", "div", "rem", "and", "or", "xor", "shl", "shr", "ushr",
            "less", "lessOrEqual", "greater", "greaterOrEqual"
        )
        private val allowedMethodsOnStrings = setOf(
            "<get-length>", "plus", "get", "compareTo", "equals", "toString"
        )
        private val allowedExtensionFunctions = setOf(
            "kotlin.floorDiv", "kotlin.mod", "kotlin.NumbersKt.floorDiv", "kotlin.NumbersKt.mod", "kotlin.<get-code>"
        )
        private val allowedBuiltinExtensionFunctions = listOf(
            BuiltInOperatorNames.LESS, BuiltInOperatorNames.LESS_OR_EQUAL,
            BuiltInOperatorNames.GREATER, BuiltInOperatorNames.GREATER_OR_EQUAL,
            BuiltInOperatorNames.EQEQ, BuiltInOperatorNames.IEEE754_EQUALS,
            BuiltInOperatorNames.ANDAND, BuiltInOperatorNames.OROR
        ).map { IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier(it)).asString() }.toSet()

        private val allowedOriginsForWhen = setOf(IrStatementOrigin.ANDAND, IrStatementOrigin.OROR)

        override fun canEvaluateFunction(function: IrFunction): Boolean {
            if (function.property.isConst) return true

            val returnType = function.returnType
            if (!returnType.isPrimitiveType() && !returnType.isString() && !returnType.isUnsignedType()) return false

            val fqName = function.fqNameWhenAvailable?.asString()
            val parent = function.parentClassOrNull
            val parentType = parent?.defaultType
            return when {
                parentType == null -> fqName in allowedExtensionFunctions || fqName in allowedBuiltinExtensionFunctions
                parentType.isPrimitiveType() -> function.name.asString() in allowedMethodsOnPrimitives
                parentType.isString() -> function.name.asString() in allowedMethodsOnStrings
                parent.isObject -> parent.parentClassOrNull?.defaultType?.let { it.isPrimitiveType() || it.isUnsigned() } == true
                parentType.isUnsignedType() && function is IrConstructor -> true
                else -> fqName in allowedExtensionFunctions || fqName in allowedBuiltinExtensionFunctions
            }
        }

        override fun canEvaluateBlock(block: IrBlock): Boolean = block.statements.size == 1
        override fun canEvaluateExpression(expression: IrExpression): Boolean {
            return when {
                expression is IrWhen -> expression.origin in allowedOriginsForWhen
                expression !is IrCall -> false
                expression.hasUnsignedArgs() -> expression.symbol.owner.fqNameWhenAvailable?.asString() == "kotlin.String.plus"
                else -> true
            }
        }

        private fun IrCall.hasUnsignedArgs(): Boolean {
            fun IrExpression?.hasUnsignedType() = this != null && type.isUnsigned()
            if (dispatchReceiver.hasUnsignedType() || extensionReceiver.hasUnsignedType()) return true
            if ((0 until this.valueArgumentsCount).any { getValueArgument(it)?.type?.isUnsigned() == true }) return true
            return false
        }
    },

    ONLY_INTRINSIC_CONST {
        override fun canEvaluateFunction(function: IrFunction): Boolean {
            return function.isCompileTimePropertyAccessor() || function.isMarkedAsIntrinsicConstEvaluation()
        }

        private fun IrFunction?.isCompileTimePropertyAccessor(): Boolean {
            val property = this?.property ?: return false
            return property.isConst || property.isMarkedAsIntrinsicConstEvaluation()
        }

        override fun canEvaluateBlock(block: IrBlock): Boolean = block.origin == IrStatementOrigin.WHEN || block.statements.size == 1

        override fun canEvaluateExpression(expression: IrExpression): Boolean {
            return ONLY_BUILTINS.canEvaluateExpression(expression) || expression is IrWhen
        }
    };

    open fun canEvaluateFunction(function: IrFunction): Boolean = false
    open fun canEvaluateEnumValue(enumEntry: IrGetEnumValue): Boolean = false
    open fun canEvaluateFunctionExpression(expression: IrFunctionExpression): Boolean = false
    open fun canEvaluateCallableReference(reference: IrCallableReference<*>): Boolean = false
    open fun canEvaluateClassReference(reference: IrDeclarationReference): Boolean = false

    open fun canEvaluateBlock(block: IrBlock): Boolean = false
    open fun canEvaluateComposite(composite: IrComposite): Boolean {
        return composite.origin == IrStatementOrigin.DESTRUCTURING_DECLARATION || composite.origin == null
    }

    open fun canEvaluateExpression(expression: IrExpression): Boolean = false

    open fun mustCheckBodyOf(function: IrFunction): Boolean {
        return function.property != null
    }

    protected fun IrDeclaration.isMarkedAsIntrinsicConstEvaluation() = isMarkedWith(intrinsicConstEvaluationAnnotation)

    protected fun IrDeclaration.isMarkedWith(annotation: FqName): Boolean {
        if (this is IrClass && this.isCompanion) return false
        if (this.hasAnnotation(annotation)) return true
        return (this.parent as? IrClass)?.isMarkedWith(annotation) ?: false
    }
}
