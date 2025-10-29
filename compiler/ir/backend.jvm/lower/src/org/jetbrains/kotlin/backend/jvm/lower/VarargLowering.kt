/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrArrayBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * Replaces varargs with array arguments, and lowers [arrayOf] and [emptyArray] calls.
 */
@PhasePrerequisites(PolymorphicSignatureLowering::class)
internal class VarargLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    // Ignore annotations
    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructor = expression.symbol.owner
        if (constructor.constructedClass.isAnnotationClass)
            return expression
        return super.visitConstructorCall(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid()
        val function = expression.symbol

        // Replace empty varargs with empty arrays
        val irFunction = function.owner
        for ((parameter, argument) in irFunction.parameters zip expression.arguments) {
            if (argument != null) continue

            if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
                // Compute the correct type for the array argument.
                val arrayType = parameter.type.substitute(expression.typeSubstitutionMap).makeNotNull()
                // Cannot use `expression.arguments[parameter]` to get argument
                // because of the different owner in the case of polymorphic varargs calls (e.g. `invokeExact` call)
                expression.arguments[parameter.indexInParameters] = createBuilder().irArrayOf(arrayType)
            }
        }

        return expression
    }

    override fun visitVararg(expression: IrVararg): IrExpression =
        createBuilder(expression.startOffset, expression.endOffset).irArray(expression.type) { addVararg(expression) }

    private fun IrArrayBuilder.addVararg(expression: IrVararg) {
        loop@ for (element in expression.elements) {
            when (element) {
                is IrExpression -> +element.transform(this@VarargLowering, null)
                is IrSpreadElement -> {
                    val spread = element.expression
                    // Don't copy immediately created arrays
                    // TODO: what about `emptyArray`?
                    val arrayOfArgumentOfSpread = (spread as? IrFunctionAccessExpression)?.arrayOfVarargArgument
                    if (arrayOfArgumentOfSpread is IrVararg) {
                        addVararg(arrayOfArgumentOfSpread)
                        continue@loop
                    }
                    addSpread(spread.transform(this@VarargLowering, null))
                }
                else -> error("Unexpected IrVarargElement subclass: $element")
            }
        }
    }

    private fun createBuilder(startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET) =
        context.createJvmIrBuilder(currentScope!!, startOffset, endOffset)

}

internal val PRIMITIVE_ARRAY_OF_NAMES: Set<String> =
    (PrimitiveType.entries.map { type -> type.name } + UnsignedType.entries.map { type -> type.typeName.asString() })
        .map { name -> name.toLowerCaseAsciiOnly() + "ArrayOf" }.toSet()

internal const val ARRAY_OF_NAME = "arrayOf"
internal const val ARRAY_COMPANION_OF_NAME = "of"

internal fun IrFunction.isArrayOf(): Boolean {
    val parent = when (val directParent = parent) {
        is IrClass -> directParent.getPackageFragment()
        is IrPackageFragment -> directParent
        else -> return false
    }
    return parent.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME &&
            name.asString().let { it in PRIMITIVE_ARRAY_OF_NAMES || it == ARRAY_OF_NAME } &&
            hasShape(regularParameters = 1) &&
            parameters[0].isVararg
}

internal fun IrFunction.isArrayCompanionOf(): Boolean {
    if (name.asString() != ARRAY_COMPANION_OF_NAME) return false
    val companion = (parent as? IrClass)?.takeIf { it.isCompanion } ?: return false
    val companionOwner = companion.parent as? IrClass ?: return false

    // We don't consider `uintArrayOf` an intrinsic and rely on @InlineOnly annotation only.
    // However, that doesn't work with `UIntArray.of` because the instance of `UIntArray.Companion`
    // must be additionally obtained in that case. Therefore, `UIntArray.of` is considered an intrinsic.
    return companionOwner.defaultType.run { isBoxedArray || isPrimitiveArray() || isUnsignedArray() }
            && hasShape(dispatchReceiver = true, regularParameters = 1)
            && parameters[1].isVararg
}

/**
 * @return If this call is to `(type)ArrayOf` or `(Type)Array.Companion.of` intrinsic, its vararg argument. Otherwise, `null`.
 */
internal val IrFunctionAccessExpression.arrayOfVarargArgument: IrExpression?
    get() {
        val callee = symbol.owner

        fun argument(idx: Int): IrExpression =
            arguments[idx] ?: throw AssertionError("Argument #$idx expected: ${dump()}")

        return when {
            callee.isArrayOf() -> argument(0)
            // the first parameter is `(Type)Array.Companion`
            callee.isArrayCompanionOf() -> argument(1)
            else -> null
        }
    }

internal fun IrFunction.isEmptyArray(): Boolean = isTopLevelInPackage("emptyArray", StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
