/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * Lowers `::prop.isInitialized` on a top-level `lateinit` property to a call of a generated
 * private top-level function, so that evaluating the check from a class member triggers the
 * file's lazy properties initialization — like a static field read triggers the file facade
 * `<clinit>` on the JVM. See KT-91246.
 *
 * The generated function is an ordinary top-level function, so the pre-existing logic of
 * [PropertyLazyInitLowering] prepends the file initialization call to its body:
 *
 * ```kotlin
 * // class member: val check: Boolean get() = ::late.isInitialized
 * private fun late$isInitialized(): Boolean {
 *     <init properties>() // prepended by PropertyLazyInitLowering
 *     return late != null
 * }
 * class Foo {
 *     val check: Boolean get() = late$isInitialized()
 * }
 * ```
 *
 * By the time this pass runs, [org.jetbrains.kotlin.backend.common.lower.LateinitLowering] has
 * already turned `::late.isInitialized` into a raw `late != null` field check — both in the
 * current module and in dependency klibs — so the pass matches that pattern. A raw read of a
 * top-level `lateinit` backing field cannot come from user code: ordinary accesses and
 * comparisons go through the throwing getter.
 */
class WebLateinitIsInitializedLowering(
    private val context: JsCommonBackendContext
) : BodyLoweringPass, IrElementTransformerVoid() {

    private val isInitializedFunctions = HashMap<IrPropertySymbol, IrSimpleFunction>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrFunction || container.parent !is IrClass) return
        irBody.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val property = expression.topLevelLateinitPropertyOrNullCheckedForNull() ?: return expression

        val isInitializedFunction = isInitializedFunctions.getOrPut(property.symbol) {
            createIsInitializedFunction(property)
        }

        return context.irBuiltIns.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).run {
            irCall(isInitializedFunction.symbol, context.irBuiltIns.booleanType)
        }
    }

    /**
     * Returns the top-level lateinit property whose backing field is null-checked by the receiver call,
     * or null if it is not the `!(field == null)` check produced by
     * [org.jetbrains.kotlin.backend.common.lower.LateinitLowering].
     */
    private fun IrCall.topLevelLateinitPropertyOrNullCheckedForNull(): IrProperty? {
        if (origin != IrStatementOrigin.EXCLEQ) return null

        val equalsCall = arguments[0] as? IrCall ?: return null
        if (equalsCall.origin != IrStatementOrigin.EXCLEQ) return null

        val getField = equalsCall.arguments.firstNotNullOfOrNull { it as? IrGetField } ?: return null
        if (getField.receiver != null) return null
        if (equalsCall.arguments.count { it.isNullConst() } != 1) return null

        val property = getField.symbol.owner.correspondingPropertySymbol?.owner ?: return null
        if (!property.isLateinit || property.parent !is IrFile) return null

        return property
    }

    private fun IrExpression?.isNullConst(): Boolean =
        this is IrConst && kind == IrConstKind.Null

    private fun createIsInitializedFunction(property: IrProperty): IrSimpleFunction {
        val backingField = property.backingField!!

        return context.irFactory.stageController.restrictTo(property) {
            context.irFactory.addFunction(property.parent as IrFile) {
                // PropertyLazyInitLowering prepends the file initialization call only
                // to functions with one of a fixed set of declaration origins.
                origin = IrDeclarationOrigin.DEFINED
                name = Name.identifier("${property.name.asString()}\$isInitialized")
                visibility = DescriptorVisibilities.PRIVATE
                returnType = context.irBuiltIns.booleanType
            }.apply {
                body = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET).irBlockBody {
                    +irReturn(irNotEquals(irGetField(null, backingField), irNull()))
                }
            }
        }
    }
}
