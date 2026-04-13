/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.*

/**
 * This lowering deduplicates callable reference implementation functions, replacing all duplicates call with calls to canonical one.
 * The first found callable reference implementation for a given function is considered a canonical one.
 *
 * Callable reference functions are considered duplicates if all the below statements are true:
 * 1. Both target the same [IrFunctionSymbol]
 * 2. Both have identical bound value types
 * 3. Both have the same base KFunctionImpl type
 * 4. Both have identical conversion flags (hasUnitConversion, hasSuspendConversion, hasVarargConversion, isRestrictedSuspension)
 */
@PhasePrerequisites(MoveCallableFactoriesToDeclarationsLowering::class)
class DeduplicateCallableReferenceFactoriesLowering(private val context: JsIrBackendContext) : ModuleLoweringPass {
    private val canonicalCallableFactories = hashMapOf<CallableFunctionReferenceId, IrSimpleFunction>()
    private val duplicatingFactories = hashSetOf<IrSimpleFunction>()

    override fun lower(irModule: IrModuleFragment) {
        val collector = object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                declaration.acceptChildrenVoid(this)
                if (declaration.origin != JsStatementOrigins.FACTORY_ORIGIN) return

                val callable = declaration.richFunctionReference ?: return
                val key = createKey(callable) ?: return
                val canonicalFactory = canonicalCallableFactories[key]
                when {
                    canonicalFactory == null -> canonicalCallableFactories[key] = declaration
                    declaration != canonicalFactory -> duplicatingFactories += declaration
                }
            }
        }
        for (irFile in irModule.files)
            irFile.acceptVoid(collector)

        if (duplicatingFactories.isEmpty()) return

        val callRewriter = object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                val function = expression.symbol.owner
                if (function.origin != JsStatementOrigins.FACTORY_ORIGIN) return expression

                val callable = function.richFunctionReference ?: return expression
                val key = createKey(callable) ?: return expression
                val canonicalFactory = canonicalCallableFactories[key] ?: return expression
                expression.symbol = canonicalFactory.symbol

                return expression
            }

            override fun visitFile(declaration: IrFile): IrFile {
                declaration.transformChildrenVoid()
                declaration.removeDuplicatingFactories()
                return declaration
            }

            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid()
                declaration.removeDuplicatingFactories()
                return declaration
            }

            private fun IrDeclarationContainer.removeDuplicatingFactories() {
                declarations.removeAll { it is IrSimpleFunction && it in duplicatingFactories }
            }
        }
        irModule.transformChildrenVoid(callRewriter)
    }

    private fun createKey(reference: IrRichFunctionReference): CallableFunctionReferenceId? {
        return reference.reflectionTargetSymbol?.let { target ->
            CallableFunctionReferenceId(
                target,
                reference.boundValues.map { it.type },
                reference.type,
                reference.hasUnitConversion,
                reference.hasSuspendConversion,
                reference.hasVarargConversion,
                reference.isRestrictedSuspension
            )
        }
    }

    private data class CallableFunctionReferenceId(
        val targetSymbol: IrFunctionSymbol,
        val boundValueTypes: List<IrType>,
        val callableBaseType: IrType,
        val hasUnitConversion: Boolean,
        val hasSuspendConversion: Boolean,
        val hasVarargConversion: Boolean,
        val isRestrictedSuspension: Boolean
    )
}