/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnsignedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName

enum class EvaluationMode(protected val mustCheckBody: Boolean) {
    FULL(mustCheckBody = true) {
        override fun canEvaluateFunction(function: IrFunction, context: IrCall?): Boolean = true
        override fun canEvaluateEnumValue(enumEntry: IrGetEnumValue, context: IrCall?): Boolean = true
        override fun canEvaluateReference(reference: IrCallableReference<*>, context: IrCall?): Boolean = true
    },

    WITH_ANNOTATIONS(mustCheckBody = false) {
        override fun canEvaluateFunction(function: IrFunction, context: IrCall?): Boolean {
            if (function.isCompileTimePropertyAccessor()) return true
            return function.isMarkedAsCompileTime() || function.origin == IrBuiltIns.BUILTIN_OPERATOR ||
                    (function is IrSimpleFunction && function.isOperator && function.name.asString() == "invoke") ||
                    (function is IrSimpleFunction && function.isFakeOverride && function.overriddenSymbols.any { canEvaluateFunction(it.owner) }) ||
                    function.isCompileTimeTypeAlias()
        }

        private fun IrFunction?.isCompileTimePropertyAccessor(): Boolean {
            val property = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: return false
            if (property.isConst) return true
            if (property.isMarkedAsCompileTime() || property.isCompileTimeTypeAlias()) return true

            val backingField = property.backingField
            val backingFieldExpression = backingField?.initializer?.expression as? IrGetValue
            return backingFieldExpression?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
        }

        override fun canEvaluateEnumValue(enumEntry: IrGetEnumValue, context: IrCall?): Boolean = true
        override fun canEvaluateReference(reference: IrCallableReference<*>, context: IrCall?): Boolean = true
    },

    ONLY_BUILTINS(mustCheckBody = false) {
        private val forbiddenMethodsOnPrimitives = setOf("inc", "dec", "rangeTo", "rangeUntil", "hashCode")
        private val forbiddenMethodsOnStrings = setOf("subSequence", "hashCode", "<init>")
        private val allowedExtensionFunctions = setOf(
            "kotlin.floorDiv", "kotlin.mod", "kotlin.NumbersKt.floorDiv", "kotlin.NumbersKt.mod", "kotlin.<get-code>",
            "kotlin.internal.ir.EQEQ", "kotlin.internal.ir.ieee754equals",
        )

        override fun canEvaluateFunction(function: IrFunction, context: IrCall?): Boolean {
            if ((function as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.isConst == true) return true

            val fqName = function.fqNameWhenAvailable?.asString()
            val parent = function.parentClassOrNull
            val parentType = parent?.defaultType
            return when {
                parentType == null -> fqName in allowedExtensionFunctions
                parentType.isPrimitiveType() -> function.name.asString() !in forbiddenMethodsOnPrimitives
                parentType.isString() -> function.name.asString() !in forbiddenMethodsOnStrings
                parentType.isAny() -> function.name.asString() == "toString" && context?.dispatchReceiver !is IrGetObjectValue
                parent.isObject -> parent.parentClassOrNull?.defaultType?.let { it.isPrimitiveType() || it.isUnsigned() } == true
                parentType.isUnsignedType() && function is IrConstructor -> true
                else -> fqName in allowedExtensionFunctions
            }
        }

        override fun canEvaluateEnumValue(enumEntry: IrGetEnumValue, context: IrCall?): Boolean = false
        override fun canEvaluateReference(reference: IrCallableReference<*>, context: IrCall?): Boolean = false
    },

    ONLY_INTRINSIC_CONST(mustCheckBody = false) {
        override fun canEvaluateFunction(function: IrFunction, context: IrCall?): Boolean {
            return function.isCompileTimePropertyAccessor() || function.isMarkedAsIntrinsicConstEvaluation() || context.isIntrinsicConstEvaluationNameProperty()
        }

        private fun IrFunction?.isCompileTimePropertyAccessor(): Boolean {
            val property = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: return false
            return property.isConst || (property.resolveFakeOverride() ?: property).isMarkedAsIntrinsicConstEvaluation()
        }

        override fun canEvaluateEnumValue(enumEntry: IrGetEnumValue, context: IrCall?): Boolean {
            return context.isIntrinsicConstEvaluationNameProperty()
        }

        override fun canEvaluateReference(reference: IrCallableReference<*>, context: IrCall?): Boolean {
            return context.isIntrinsicConstEvaluationNameProperty()
        }

        private fun IrCall?.isIntrinsicConstEvaluationNameProperty(): Boolean {
            if (this == null) return false
            val owner = this.symbol.owner
            val property = (owner as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: return false
            return owner.isCompileTimePropertyAccessor() && property.name.asString() == "name"
        }
    };

    abstract fun canEvaluateFunction(function: IrFunction, context: IrCall? = null): Boolean
    abstract fun canEvaluateEnumValue(enumEntry: IrGetEnumValue, context: IrCall? = null): Boolean
    abstract fun canEvaluateReference(reference: IrCallableReference<*>, context: IrCall? = null): Boolean

    fun mustCheckBodyOf(function: IrFunction): Boolean {
        if (function is IrSimpleFunction && function.correspondingPropertySymbol != null) return true
        return (mustCheckBody || function.isLocal) && !function.isContract() && !function.isMarkedAsEvaluateIntrinsic()
    }

    protected val compileTimeTypeAliases = setOf(
        "java.lang.StringBuilder", "java.lang.IllegalArgumentException", "java.util.NoSuchElementException"
    )

    fun IrDeclaration.isMarkedAsCompileTime() = isMarkedWith(compileTimeAnnotation)
    protected fun IrDeclaration.isMarkedAsIntrinsicConstEvaluation() = isMarkedWith(intrinsicConstEvaluationAnnotation)
    private fun IrDeclaration.isContract() = isMarkedWith(contractsDslAnnotation)
    private fun IrDeclaration.isMarkedAsEvaluateIntrinsic() = isMarkedWith(evaluateIntrinsicAnnotation)
    protected fun IrDeclaration.isCompileTimeTypeAlias() = this.parentClassOrNull?.fqName in compileTimeTypeAliases

    protected fun IrDeclaration.isMarkedWith(annotation: FqName): Boolean {
        if (this is IrClass && this.isCompanion) return false
        if (this.hasAnnotation(annotation)) return true
        return (this.parent as? IrClass)?.isMarkedWith(annotation) ?: false
    }
}