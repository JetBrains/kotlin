/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InnerClassesLowering(val context: BackendContext) : DeclarationTransformer {

    override fun lower(irFile: IrFile) {
        runPostfix(true).toFileLoweringPass().lower(irFile)
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {

        if (declaration is IrClass && declaration.isInner) {
            stageController.unrestrictDeclarationListsAccess {
                declaration.declarations += context.declarationFactory.getOuterThisField(declaration)
            }
        } else if (declaration is IrConstructor) {
            val irClass = declaration.parentAsClass
            if (!irClass.isInner) return null

            val newConstructor = lowerConstructor(declaration)
            val oldConstructorParameterToNew = context.primaryConstructorParameterMap(declaration)
            for ((oldParam, newParam) in oldConstructorParameterToNew.entries) {
                newParam.defaultValue = oldParam.defaultValue?.let { oldDefault ->
                    IrExpressionBodyImpl(oldDefault.startOffset, oldDefault.endOffset) {
                        expression = oldDefault.expression.patchDeclarationParents(newConstructor)
                    }
                }
            }

            return listOf(newConstructor)
        }

        return null
    }

    private fun lowerConstructor(irConstructor: IrConstructor): IrConstructor {
        val loweredConstructor = context.declarationFactory.getInnerClassConstructorWithOuterThisParameter(irConstructor)
        val outerThisParameter = loweredConstructor.valueParameters[0]

        val irClass = irConstructor.parentAsClass
        val parentThisField = context.declarationFactory.getOuterThisField(irClass)

        val blockBody = irConstructor.body as? IrBlockBody ?: throw AssertionError("Unexpected constructor body: ${irConstructor.body}")

        loweredConstructor.body = IrBlockBodyImpl(blockBody.startOffset, blockBody.endOffset) {
            context.createIrBuilder(irConstructor.symbol, irConstructor.startOffset, irConstructor.endOffset).apply {
                statements.add(0, irSetField(irGet(irClass.thisReceiver!!), parentThisField, irGet(outerThisParameter)))
            }

            statements.addAll(blockBody.statements)

            if (statements.find { it is IrInstanceInitializerCall } == null) {
                val delegatingConstructorCall =
                    statements.find { it is IrDelegatingConstructorCall } as IrDelegatingConstructorCall?
                        ?: throw AssertionError("Delegating constructor call expected: ${irConstructor.dump()}")
                delegatingConstructorCall.apply { dispatchReceiver = IrGetValueImpl(startOffset, endOffset, outerThisParameter.symbol) }
            }
            patchDeclarationParents(loweredConstructor)

            val oldConstructorParameterToNew = context.primaryConstructorParameterMap(irConstructor)
            transformChildrenVoid(VariableRemapper(oldConstructorParameterToNew))
        }

        return loweredConstructor
    }

}

private fun BackendContext.primaryConstructorParameterMap(originalConstructor: IrConstructor): Map<IrValueParameter, IrValueParameter> {
    val oldConstructorParameterToNew = HashMap<IrValueParameter, IrValueParameter>()

    val loweredConstructor = declarationFactory.getInnerClassConstructorWithOuterThisParameter(originalConstructor)

    originalConstructor.valueParameters.forEach { old ->
        oldConstructorParameterToNew[old] = loweredConstructor.valueParameters[old.index + 1]
    }

    return oldConstructorParameterToNew
}


class InnerClassesMemberBodyLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, true)
    }

    private val IrValueSymbol.classForImplicitThis: IrClass?
        // TODO: is this the correct way to get the class?
        get() =
            if (this is IrValueParameterSymbol && owner.index == -1 &&
                (owner == (owner.parent as? IrFunction)?.dispatchReceiverParameter ||
                        owner == (owner.parent as? IrClass)?.thisReceiver)
            ) {
                owner.type.classOrNull?.owner
            } else
                null

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val irClass = container.parent as? IrClass ?: return

        if (!irClass.isInner) return

        if (container is IrField || container is IrAnonymousInitializer || container is IrValueParameter) {
            val primaryConstructor = context.declarationFactory.getInnerClassOriginalPrimaryConstructorOrNull(irClass)
            if (primaryConstructor != null) {
                val oldConstructorParameterToNew = context.primaryConstructorParameterMap(primaryConstructor)
                irBody.transformChildrenVoid(VariableRemapper(oldConstructorParameterToNew))
            }
        }

        irBody.fixThisReference(irClass, container)
    }

    fun IrBody.fixThisReference(irClass: IrClass, container: IrDeclaration) {
        val enclosingFunction: IrDeclaration? = run {
            var current: IrDeclaration? = container
            while (current != null && current !is IrFunction) {
                current = current.parent as? IrDeclaration
            }
            current
        }
        transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitClassNew(declaration: IrClass): IrStatement =
                declaration

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                expression.transformChildrenVoid(this)

                val implicitThisClass = expression.symbol.classForImplicitThis
                if (implicitThisClass == null || implicitThisClass == irClass) return expression

                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val origin = expression.origin
                val function = (currentFunction?.irElement ?: enclosingFunction) as? IrFunction
                val enclosingThisReceiver = function?.dispatchReceiverParameter ?: irClass.thisReceiver!!

                var irThis: IrExpression = IrGetValueImpl(startOffset, endOffset, enclosingThisReceiver.symbol, origin)
                var innerClass = irClass
                while (innerClass != implicitThisClass) {
                    if (!innerClass.isInner) {
                        // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                        // should be transformed by closures conversion.
                        return expression
                    }

                    irThis = if (function is IrConstructor && irClass == innerClass) {
                        // Might be before a super() call (e.g. an argument to one), in which case the JVM bytecode verifier will reject
                        // an attempt to access the field. Good thing we have a local variable as well.
                        IrGetValueImpl(startOffset, endOffset, function.valueParameters[0].symbol, origin)
                    } else {
                        val outerThisField = context.declarationFactory.getOuterThisField(innerClass)
                        IrGetFieldImpl(startOffset, endOffset, outerThisField.symbol, outerThisField.type, irThis, origin)
                    }
                    innerClass = innerClass.parentAsClass
                }
                return irThis
            }
        })
    }
}

val innerClassConstructorCallsPhase = makeIrFilePhase(
    ::InnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCalls",
    description = "Handle constructor calls for inner classes"
)

class InnerClassConstructorCallsLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val dispatchReceiver = expression.dispatchReceiver ?: return expression
                val callee = expression.symbol
                val parent = callee.owner.parentAsClass
                if (!parent.isInner) return expression

                val newCallee = context.declarationFactory.getInnerClassConstructorWithOuterThisParameter(callee.owner)
                val classTypeParametersCount = expression.typeArgumentsCount - expression.constructorTypeArgumentsCount
                val newCall = IrConstructorCallImpl.fromSymbolOwner(
                    expression.startOffset, expression.endOffset, expression.type, newCallee.symbol, classTypeParametersCount, expression.origin
                )

                newCall.copyTypeArgumentsFrom(expression)
                newCall.putValueArgument(0, dispatchReceiver)
                for (i in 1..newCallee.valueParameters.lastIndex) {
                    newCall.putValueArgument(i, expression.getValueArgument(i - 1))
                }

                return newCall
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val dispatchReceiver = expression.dispatchReceiver ?: return expression
                val classConstructor = expression.symbol.owner
                if (!classConstructor.parentAsClass.isInner) return expression

                val newCallee = context.declarationFactory.getInnerClassConstructorWithOuterThisParameter(classConstructor)
                val newCall = IrDelegatingConstructorCallImpl(
                    expression.startOffset, expression.endOffset, context.irBuiltIns.unitType, newCallee.symbol, expression.typeArgumentsCount
                ).apply { copyTypeArgumentsFrom(expression) }

                newCall.putValueArgument(0, dispatchReceiver)
                for (i in 1..newCallee.valueParameters.lastIndex) {
                    newCall.putValueArgument(i, expression.getValueArgument(i - 1))
                }

                return newCall
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                val callee = expression.symbol as? IrConstructorSymbol ?: return expression
                val parent = callee.owner.parent as? IrClass ?: return expression
                if (!parent.isInner) return expression

                val newCallee = context.declarationFactory.getInnerClassConstructorWithOuterThisParameter(callee.owner)
                val newReflectionTarget = expression.reflectionTarget?.let { reflectionTarget ->
                    if (reflectionTarget is IrConstructorSymbol) {
                        context.declarationFactory.getInnerClassConstructorWithOuterThisParameter(reflectionTarget.owner)
                    } else {
                        null
                    }
                }

                val newReference = expression.run {
                    IrFunctionReferenceImpl(
                        startOffset,
                        endOffset,
                        type,
                        newCallee.symbol,
                        typeArgumentsCount,
                        newReflectionTarget?.symbol,
                        origin
                    )
                }

                newReference.let {
                    it.copyTypeArgumentsFrom(expression)
                    // TODO: This is wrong, since we moved all parameters into value parameters,
                    //       but changing it breaks JS IR in CallableReferenceLowering.
                    it.dispatchReceiver = expression.dispatchReceiver
                    it.extensionReceiver = expression.extensionReceiver
                    for (v in 0 until expression.valueArgumentsCount) {
                        it.putValueArgument(v, expression.getValueArgument(v))
                    }
                }

                return newReference
            }
            // TODO callable references?
        })
    }
}

