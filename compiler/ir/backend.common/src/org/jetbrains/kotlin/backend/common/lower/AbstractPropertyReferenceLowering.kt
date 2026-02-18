/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources
import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.selectSAMOverriddenFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

abstract class AbstractPropertyReferenceLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {
    private val irBuiltIns = context.irBuiltIns

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol)
                val originalPropertySymbol = expression.reflectionTargetSymbol
                if (originalPropertySymbol is IrLocalDelegatedPropertySymbol) {
                    return irBuilder.createLocalKProperty(
                        expression, originalPropertySymbol.owner.name.asString(), expression.type,
                        isMutable = expression.setterFunction != null
                    )
                }
                require(originalPropertySymbol is IrPropertySymbol)
                val typeArguments = (expression.type as IrSimpleType).arguments.map { it.typeOrNull ?: irBuiltIns.anyNType }
                val block = irBuilder.irBlock {
                    val getterReference = irRichFunctionReference(
                        function = expression.getterFunction,
                        superType = functionReferenceClass(typeArguments.size - 1).typeWith(typeArguments),
                        reflectionTarget = originalPropertySymbol.owner.getter!!.symbol,
                        captures = expression.boundValues,
                        origin = expression.origin,
                    ).apply {
                        reflectionTargetLinkageError = expression.reflectionTargetLinkageError
                    }
                    val setterReference = expression.setterFunction?.let { setterFunction ->
                        // we need to avoid calculation of bound values twice, so store them to temp variables
                        val tempVars = getterReference.boundValues.map {
                            if (it is IrGetValue) it.symbol.owner else irTemporary(it)
                        }
                        getterReference.boundValues.clear()
                        getterReference.boundValues += tempVars.map { irGet(it) }
                        irRichFunctionReference(
                            function = setterFunction,
                            superType = functionReferenceClass(typeArguments.size).typeWith(typeArguments + irBuiltIns.unitType),
                            reflectionTarget = originalPropertySymbol.owner.setter!!.symbol,
                            captures = tempVars.map { irGet(it) },
                            origin = expression.origin,
                        ).apply {
                            reflectionTargetLinkageError = expression.reflectionTargetLinkageError
                        }
                    }
                    +createKProperty(expression, typeArguments, getterReference, setterReference)
                }
                if (expression.boundValues.isEmpty()) {
                    return block.statements.single() as IrExpression
                }
                return block
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                shouldNotBeCalled("Property references should've been lowered at this point")
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                shouldNotBeCalled("Property references should've been lowered at this point")
            }

            private fun IrBuilderWithScope.irRichFunctionReference(
                function: IrSimpleFunction,
                superType: IrSimpleType,
                reflectionTarget: IrSimpleFunctionSymbol,
                captures: List<IrExpression>,
                origin: IrStatementOrigin?,
            ): IrRichFunctionReferenceImpl = irRichFunctionReference(
                invokeFunction = function,
                superType = superType,
                reflectionTargetSymbol = reflectionTarget,
                overriddenFunctionSymbol = superType.classOrFail.owner.selectSAMOverriddenFunction().symbol,
                captures = captures,
                origin = origin,
            )
        })
    }

    protected fun IrBuilderWithScope.propertyReferenceNameExpression(reference: IrRichPropertyReference): IrExpression {
        val originalPropertySymbol = reference.reflectionTargetSymbol
        require(originalPropertySymbol is IrPropertySymbol)
        return irString(originalPropertySymbol.owner.name.asString())
    }

    protected fun IrBuilderWithScope.propertyReferenceLinkageErrorExpression(
        reference: IrRichPropertyReference,
        defaultValue: () -> IrExpression = ::irNull,
    ): IrExpression =
        reference.reflectionTargetLinkageError?.let {
            this@AbstractPropertyReferenceLowering.context.partialLinkageSupport.prepareLinkageError(
                doNotLog = true,
                it,
                reference,
                PartialLinkageSources.File.determineFileFor(reference.getterFunction),
            )
        }?.let(::irString) ?: defaultValue()

    abstract fun functionReferenceClass(arity: Int): IrClassSymbol

    abstract fun IrBuilderWithScope.createKProperty(
        reference: IrRichPropertyReference,
        typeArguments: List<IrType>,
        getterReference: IrRichFunctionReference,
        setterReference: IrRichFunctionReference?,
    ): IrExpression

    abstract fun IrBuilderWithScope.createLocalKProperty(
        reference: IrRichPropertyReference,
        propertyName: String,
        propertyType: IrType,
        isMutable: Boolean,
    ): IrExpression
}
