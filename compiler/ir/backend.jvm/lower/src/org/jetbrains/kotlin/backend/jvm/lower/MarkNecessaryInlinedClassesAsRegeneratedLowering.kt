/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins.INLINED_FUNCTION_REFERENCE
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.inlineDeclaration
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Scans all inlined functions and marks anonymous objects that must be later regenerated at backend.
 */
@PhaseDescription(
    name = "MarkNecessaryInlinedClassesAsRegeneratedLowering",
    prerequisite = [JvmIrInliner::class, CreateSeparateCallForInlinedLambdasLowering::class]
)
internal class MarkNecessaryInlinedClassesAsRegeneratedLowering(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    private var IrDeclaration.wasVisitedForRegenerationLowering: Boolean by irFlag(false)

    override fun lower(irFile: IrFile) {
        if (context.config.enableIrInliner) {
            irFile.acceptChildrenVoid(this)
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
        if (inlinedBlock.isFunctionInlining()) {
            val element = inlinedBlock.inlineDeclaration
            if (!element.wasVisitedForRegenerationLowering) {
                // Note: functions from other module will not be affected here, they are loaded as IrLazy declarations.
                // BUT during IR serialization support we need to carefully test this logic.
                element.wasVisitedForRegenerationLowering = true
                element.acceptVoid(this)
            }

            val mustBeRegenerated = inlinedBlock.collectDeclarationsThatMustBeRegenerated()
            inlinedBlock.setUpCorrectAttributesForAllInnerElements(mustBeRegenerated)
            return
        }

        super.visitInlinedFunctionBlock(inlinedBlock)
    }

    private fun IrInlinedFunctionBlock.collectDeclarationsThatMustBeRegenerated(): Set<IrAttributeContainer> {
        val classesToRegenerate = mutableSetOf<IrAttributeContainer>()
        this.acceptVoid(object : IrElementVisitorVoid {
            private val containersStack = mutableListOf<IrAttributeContainer>()
            private val inlinableParameters = mutableListOf<IrValueParameter>()
            private val reifiedArguments = mutableListOf<IrType>()
            private var processingBeforeInlineDeclaration = false

            private fun IrExpression?.isInlinable(): Boolean {
                return this is IrFunctionExpression || this?.isLambdaBlock() == true
            }

            fun IrInlinedFunctionBlock.getInlinableParameters(): List<IrValueParameter> {
                val callee = this.inlineDeclaration
                if (callee !is IrFunction) return emptyList()
                // Must pass `callee` explicitly because there can be problems if call was created for fake override
                return (callee.parameters zip this.inlineCall!!.arguments)
                    .filter { (param, arg) ->
                        param.isInlineParameter() && (arg ?: param.defaultValue?.expression).isInlinable() ||
                                arg is IrGetValue && arg.symbol.owner in inlinableParameters
                    }
                    .map { it.first }
            }

            fun IrInlinedFunctionBlock.getReifiedArguments(): List<IrType> {
                val callee = this.inlineDeclaration
                if (callee !is IrFunction) return emptyList()
                return callee.typeParameters.mapIndexedNotNull { index, param ->
                    this.inlineCall!!.getTypeArgument(index)?.takeIf { param.isReified }
                }
            }

            private fun saveDeclarationsFromStackIntoRegenerationPool() {
                containersStack.forEach {
                    classesToRegenerate += it
                }
            }

            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            private fun visitAnonymousDeclaration(declaration: IrAttributeContainer) {
                containersStack += declaration
                if (declaration.hasReifiedTypeArguments(reifiedArguments)) {
                    saveDeclarationsFromStackIntoRegenerationPool()
                }
                if (!processingBeforeInlineDeclaration) {
                    processingBeforeInlineDeclaration = true
                    declaration.originalBeforeInline?.acceptChildrenVoid(this) // check if we need to save THIS declaration
                    processingBeforeInlineDeclaration = false
                }
                declaration.acceptChildrenVoid(this) // check if we need to save INNER declarations
                containersStack.removeLast()
            }

            override fun visitClass(declaration: IrClass) {
                visitAnonymousDeclaration(declaration)
            }

            override fun visitFunctionExpression(expression: IrFunctionExpression) {
                visitAnonymousDeclaration(expression)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                visitAnonymousDeclaration(expression)
            }

            override fun visitBlock(expression: IrBlock) {
                if (expression.isLambdaBlock()) {
                    val function = expression.statements.first() as? IrAttributeContainer ?: return super.visitBlock(expression)
                    val reference = expression.statements.last() as IrFunctionReference
                    containersStack += reference
                    visitAnonymousDeclaration(function)
                    containersStack.removeLast()
                }
                super.visitBlock(expression)
            }

            override fun visitGetValue(expression: IrGetValue) {
                super.visitGetValue(expression)
                if (
                    expression.symbol.owner in inlinableParameters ||
                    expression.type.getClass()?.let { classesToRegenerate.contains(it) } == true
                ) {
                    saveDeclarationsFromStackIntoRegenerationPool()
                }
            }

            override fun visitCall(expression: IrCall) {
                if (expression.symbol == context.ir.symbols.singleArgumentInlineFunction) {
                    when (val lambda = expression.getValueArgument(0)) {
                        is IrFunctionExpression -> lambda.function.acceptVoid(this)
                        else -> lambda?.acceptVoid(this) // for example IrFunctionReference
                    }
                    return
                }

                if (expression.origin == INLINED_FUNCTION_REFERENCE) {
                    saveDeclarationsFromStackIntoRegenerationPool()
                }
                super.visitCall(expression)
            }

            override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
                if (inlinedBlock.isLambdaInlining()) return super.visitInlinedFunctionBlock(inlinedBlock)

                val additionalInlinableParameters = inlinedBlock.getInlinableParameters()
                val additionalTypeArguments = inlinedBlock.getReifiedArguments()

                inlinableParameters.addAll(additionalInlinableParameters)
                reifiedArguments.addAll(additionalTypeArguments)
                super.visitContainerExpression(inlinedBlock)
                inlinableParameters.dropLast(additionalInlinableParameters.size)
                reifiedArguments.dropLast(additionalTypeArguments.size)
            }
        })
        return classesToRegenerate
    }

    private fun IrAttributeContainer.hasReifiedTypeArguments(reifiedArguments: List<IrType>): Boolean {
        var hasReified = false

        fun IrType.recursiveWalkDown(visitor: IrElementVisitorVoid) {
            hasReified = hasReified || this@recursiveWalkDown in reifiedArguments
            (this@recursiveWalkDown as? IrSimpleType)?.arguments?.forEach { it.typeOrNull?.recursiveWalkDown(visitor) }
        }

        this.acceptVoid(object : IrElementVisitorVoid {
            private val visitedClasses = mutableSetOf<IrClass>()

            override fun visitElement(element: IrElement) {
                if (hasReified) return
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (!visitedClasses.add(declaration)) return
                declaration.superTypes.forEach { it.recursiveWalkDown(this) }
                super.visitClass(declaration)
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall) {
                expression.typeOperand.takeIf { it is IrSimpleType }?.recursiveWalkDown(this)
                super.visitTypeOperator(expression)
            }

            override fun visitCall(expression: IrCall) {
                (0 until expression.typeArgumentsCount).forEach {
                    expression.getTypeArgument(it)?.recursiveWalkDown(this)
                }
                super.visitCall(expression)
            }

            override fun visitClassReference(expression: IrClassReference) {
                expression.classType.recursiveWalkDown(this)
                super.visitClassReference(expression)
            }
        })
        return hasReified
    }

    private fun IrElement.setUpCorrectAttributesForAllInnerElements(mustBeRegenerated: Set<IrAttributeContainer>) {
        this.acceptChildrenVoid(object : IrElementVisitorVoid {
            private fun checkAndSetUpCorrectAttributes(element: IrAttributeContainer) {
                when {
                    element !in mustBeRegenerated && element.originalBeforeInline != null -> element.setUpOriginalAttributes()
                    else -> element.acceptChildrenVoid(this)
                }
            }

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                checkAndSetUpCorrectAttributes(declaration)
            }

            override fun visitFunctionExpression(expression: IrFunctionExpression) {
                checkAndSetUpCorrectAttributes(expression)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                checkAndSetUpCorrectAttributes(expression)
            }
        })
    }

    private fun IrElement.setUpOriginalAttributes() {
        acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                if (element is IrAttributeContainer && element.originalBeforeInline != null) {
                    // Basically we need to generate SEQUENCE of `element.originalBeforeInline` and find the original one.
                    //  But we process nested inlined functions first, so `element.originalBeforeInline` will be processed already.
                    //  This mean that when we start to precess current container, all inner ones in SEQUENCE will already be processed.
                    element.attributeOwnerId = element.originalBeforeInline!!.attributeOwnerId
                    element.originalBeforeInline = null
                }
                element.acceptChildrenVoid(this)
            }
        })
    }
}
