/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.expression

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType

class IrExpressionTranslationVisitor(private val context: IrTranslationContext) : IrElementVisitor<JsExpression?, Unit> {
    override fun visitElement(element: IrElement, data: Unit): JsExpression? = null

    override fun visitCall(expression: IrCall, data: Unit): JsExpression? {
        val function = expression.descriptor
        val dispatchReceiver = expression.dispatchReceiver?.accept(this, Unit)
        val extensionReceiver = expression.extensionReceiver?.accept(this, Unit)
        val arguments = translateArguments(expression)

        val intrinsic = context.config.intrinsics[function]
        if (intrinsic != null) {
            return intrinsic.apply(context, expression, dispatchReceiver, extensionReceiver, arguments)
        }

        val superQualifier = expression.superQualifier
        val isLocallyAvailableProperty =
                function is PropertyAccessorDescriptor &&
                DescriptorUtils.isTopLevelDeclaration(function.correspondingProperty) &&
                function.module == context.module.descriptor
        val isExtensionPropertyAccessor = function is PropertyAccessorDescriptor && function.correspondingProperty.isExtensionProperty
        if (!JsDescriptorUtils.shouldAccessViaFunctions(function) && !isLocallyAvailableProperty && !isExtensionPropertyAccessor) {
            when (function) {
                is PropertyGetterDescriptor -> {
                    return when {
                        superQualifier != null -> {
                            val jsQualifier = context.translateAsTypeReference(superQualifier)
                            val name = context.naming.names[function.correspondingProperty]
                            buildJs {
                                "Kotlin".dotPure("callGetter")
                                        .invoke(dispatchReceiver!!, jsQualifier.dotPrototype(), name.ident.str())
                            }
                        }
                        dispatchReceiver != null -> {
                            JsNameRef(context.naming.names[function.correspondingProperty], dispatchReceiver)
                        }
                        else -> {
                            context.translateAsValueReference(function.correspondingProperty.original)
                        }
                    }
                }
                is PropertySetterDescriptor -> {
                    val statement = when {
                        superQualifier != null -> {
                            val jsQualifier = context.translateAsTypeReference(superQualifier)
                            val name = context.naming.names[function.correspondingProperty]
                            buildJs {
                                "Kotlin".dotPure("callSetter")
                                        .invoke(dispatchReceiver!!, jsQualifier.dotPrototype(), name.ident.str(), arguments[0])
                            }
                        }
                        dispatchReceiver != null -> {
                            buildJs {
                                dispatchReceiver.dot(context.naming.names[function.correspondingProperty]).assign(arguments[0])
                            }
                        }
                        else -> {
                            buildJs {
                                context.translateAsValueReference(function.correspondingProperty.original).assign(arguments[0])
                            }
                        }
                    }
                    context.addStatement(JsExpressionStatement(statement))
                    return null
                }
            }
        }

        val allArguments = if (extensionReceiver != null) listOf(extensionReceiver) + arguments else arguments

        if (superQualifier != null) {
            val superRef = context.translateAsTypeReference(superQualifier)
            val functionName = context.naming.names[function]
            return buildJs {
                superRef.dotPrototype().dotPure(functionName).dotPure("call")
                        .invoke(dispatchReceiver ?: JsNullLiteral(), *allArguments.toTypedArray())
            }.withInlineMetadata(context, expression.descriptor)
        }

        val qualifier = if (dispatchReceiver != null) {
            JsNameRef(context.naming.names[function], dispatchReceiver)
        }
        else {
            context.translateAsValueReference(function.original)
        }

        return if (function is ConstructorDescriptor && function.isPrimary) {
            JsNew(qualifier, allArguments)
        }
        else {
            JsInvocation(qualifier, allArguments).withInlineMetadata(context, expression.descriptor)
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Unit): JsExpression? {
        val constructor = expression.descriptor
        val arguments = translateArguments(expression)
        val classRef = context.naming.innerNames[constructor.constructedClass]
        val thisRef = context.translateAsValueReference(context.currentClass!!.thisAsReceiverParameter)
        context.addStatement(buildJs { statement(classRef.dot("call").invoke(thisRef, *arguments.toTypedArray())) })
        return null
    }

    private fun translateArguments(expression: IrFunctionAccessExpression): List<JsExpression> {
        val function = expression.descriptor
        return function.valueParameters.indices.asSequence()
                .map { expression.getValueArgument(it) }
                .map { it?.accept(this, Unit) ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0)) }
                .toList()
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Unit): JsExpression? {
        return when (expression.operator) {
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                val tmpVar = JsScope.declareTemporary()
                context.addStatement(buildJs { tmpVar.newVar() })
                val operand = expression.argument.accept(this, Unit) ?: return null
                val isCheck = buildJs { context.translateIsType(expression.typeOperand).invoke(tmpVar.ref().assign(operand)) }
                buildJs { JsConditional(isCheck, tmpVar.refPure(), "Kotlin".dotPure("throwCCE").invoke()) }
            }
            IrTypeOperator.SAFE_CAST -> {
                val tmpVar = JsScope.declareTemporary()
                context.addStatement(buildJs { tmpVar.newVar() })
                val operand = expression.argument.accept(this, Unit) ?: return null
                val isCheck = buildJs { context.translateIsType(expression.type).invoke(tmpVar.ref().assign(operand)) }
                buildJs { JsConditional(isCheck, tmpVar.refPure(), JsNullLiteral()) }
            }
            IrTypeOperator.INSTANCEOF -> {
                val operand = expression.argument.accept(this, Unit) ?: return null
                buildJs { context.translateIsType(expression.typeOperand).invoke(operand) }
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val operand = expression.argument.accept(this, Unit) ?: return null
                buildJs { not(context.translateIsType(expression.typeOperand).invoke(operand)) }
            }
            else -> expression.argument.accept(this, Unit)
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: Unit): JsExpression? {
        for (statement in expression.statements.dropLast(1)) {
            statement.accept(this, Unit)?.let {  context.addStatement(JsExpressionStatement(it)) }
        }
        val result = expression.statements.lastOrNull()?.accept(this, Unit)
        return if (KotlinBuiltIns.isUnit(expression.type)) {
            result?.let { context.addStatement(JsExpressionStatement(it)) }
            null
        }
        else {
            result
        }
    }

    override fun visitVariable(declaration: IrVariable, data: Unit): JsExpression? {
        val property = declaration.descriptor
        val name = context.naming.names[property]
        val initializer = declaration.initializer?.accept(this, Unit)
        context.addStatement(JsVars(JsVars.JsVar(name, initializer)))
        return null
    }

    override fun visitVararg(expression: IrVararg, data: Unit): JsExpression? =
            JsArrayLiteral(expression.elements.map { it.accept(this, Unit) ?: JsNullLiteral() })

    override fun visitFunctionReference(expression: IrFunctionReference, data: Unit): JsExpression? =
            context.translateAsValueReference(expression.descriptor)

    override fun visitGetValue(expression: IrGetValue, data: Unit): JsExpression =
            context.translateAsValueReference(expression.descriptor)

    override fun visitSetVariable(expression: IrSetVariable, data: Unit): JsExpression? {
        val lhs = context.translateAsValueReference(expression.descriptor)
        val rhs = expression.value.accept(this, data) ?: JsNullLiteral()
        context.addStatement(JsExpressionStatement(JsBinaryOperation(JsBinaryOperator.ASG, lhs, rhs)))
        return null
    }

    override fun visitGetField(expression: IrGetField, data: Unit): JsExpression? {
        val descriptor = expression.descriptor
        val container = descriptor.containingDeclaration
        val name = context.naming.backingFieldNames[descriptor]
        return if (container is ClassDescriptor) {
            val receiver = expression.receiver!!.accept(this, Unit)!!
            buildJs { receiver.dot(name) }
        }
        else {
            buildJs { name.ref() }
        }
    }

    override fun visitSetField(expression: IrSetField, data: Unit): JsExpression? {
        val descriptor = expression.descriptor
        val container = descriptor.containingDeclaration
        val name = context.naming.backingFieldNames[descriptor]

        val ref = if (container is ClassDescriptor) {
            val receiver = expression.receiver!!.accept(this, Unit)!!
            buildJs { receiver.dot(name) }
        }
        else {
            buildJs { name.ref() }
        }
        context.addStatement(buildJs { statement(ref.assign(expression.value.accept(this@IrExpressionTranslationVisitor, Unit)!!)) })

        return null
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): JsExpression? {
        val conditionStatements = mutableListOf<JsStatement>()
        val bodyStatements = mutableListOf<JsStatement>()
        val (condition, loopState) = context.nestedLoop(loop) {
            context.withStatements(bodyStatements) {
                loop.body?.accept(this, Unit)
            }
            context.withStatements(conditionStatements) {
                loop.condition.accept(this, Unit)!!
            }
        }

        val whileStatement = if (conditionStatements.isEmpty()) {
            JsWhile(condition, bodyStatements.asSingleStatement())
        }
        else {
            val body = JsBlock().apply {
                statements += conditionStatements + JsIf(JsPrefixOperation(JsUnaryOperator.NOT, condition), JsBreak()) + bodyStatements
            }
            JsWhile(JsBooleanLiteral(true), body)
        }

        if (loopState.breakStatements.isNotEmpty() || loopState.continueStatements.isNotEmpty()) {
            val labelName = loop.createLabelName()
            for (breakOrContinue in loopState.breakStatements + loopState.continueStatements) {
                breakOrContinue.label = labelName.makeRef()
            }
            JsLabel(labelName).also {
                it.statement = whileStatement
                context.addStatement(it)
            }
        }
        else {
            context.addStatement(whileStatement)
        }

        return null
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): JsExpression? {
        val bodyStatements = mutableListOf<JsStatement>()
        val conditionStatements = mutableListOf<JsStatement>()
        val (condition, loopState) = context.nestedLoop(loop) {
            context.withStatements(bodyStatements) {
                loop.body?.accept(this, Unit)
            }
            context.withStatements(conditionStatements) {
                loop.condition.accept(this, Unit)!!
            }
        }

        val statement: JsStatement = if (loopState.continueStatements.isNotEmpty() && conditionStatements.isNotEmpty()) {
            val jsContinueLabel = JsLabel(loop.createLabelName(), JsBlock(bodyStatements.toList()))
            bodyStatements.clear()
            bodyStatements += jsContinueLabel
            for (continueStatement in loopState.continueStatements) {
                continueStatement.label = jsContinueLabel.name.makeRef()
            }
            jsContinueLabel.replaceContinueWithBreak(jsContinueLabel.name)
            bodyStatements += conditionStatements

            val jsDoWhile = JsDoWhile(condition, JsBlock(bodyStatements))
            if (loopState.breakStatements.isNotEmpty()) {
                val breakLabelName = loop.createLabelName()
                for (breakStatement in loopState.breakStatements) {
                    breakStatement.label = breakLabelName.makeRef()
                }
                JsLabel(breakLabelName, jsDoWhile)
            }
            else {
                jsDoWhile
            }
        }
        else if (loopState.continueStatements.isNotEmpty() || loopState.breakStatements.isNotEmpty()) {
            val labelName = loop.createLabelName()
            for (breakOrContinue in loopState.breakStatements + loopState.continueStatements) {
                breakOrContinue.label = labelName.makeRef()
            }
            val jsDoWhile = JsDoWhile(condition, JsBlock(bodyStatements + conditionStatements))
            JsLabel(labelName, jsDoWhile)
        }
        else {
            JsDoWhile(condition, JsBlock(bodyStatements + conditionStatements))
        }

        context.addStatement(statement)
        return null
    }

    private fun IrLoop.createLabelName() = label?.let { JsScope.declareTemporaryName(it) } ?: JsScope.declareTemporary()

    override fun visitWhen(expression: IrWhen, data: Unit): JsExpression? {
        return context.savingStatements {
            val jsBranches = mutableListOf<JsIf>()

            withTemporaryVar(expression.type) { tmpVar ->
                for (branch in expression.branches) {
                    if (branch is IrElseBranch) {
                        tmpVar.translate(branch.result)
                    }
                    else {
                        val jsBranch = JsIf(branch.condition.accept(this, Unit) ?: JsBooleanLiteral(false), JsBlock(), JsBlock())
                        jsBranches += jsBranch
                        context.addStatement(jsBranch)

                        context.statements = (jsBranch.thenStatement as JsBlock).statements
                        tmpVar.translate(branch.result)

                        if (context.statements.size == 1) {
                            jsBranch.thenStatement = context.statements.first()
                        }

                        context.statements = (jsBranch.elseStatement as JsBlock).statements
                    }
                }

                for (jsBranch in jsBranches) {
                    (jsBranch.elseStatement as? JsBlock)?.let {
                        when (it.statements.size) {
                            0 -> jsBranch.elseStatement = null
                            1 -> jsBranch.elseStatement = it.statements[0]
                        }
                    }
                }
            }
        }
    }

    override fun visitBreak(jump: IrBreak, data: Unit): JsExpression? {
        context.addStatement(JsBreak().also {
            context.loops[jump.loop]!!.breakStatements += it
        })
        return null
    }

    override fun visitContinue(jump: IrContinue, data: Unit): JsExpression? {
        context.addStatement(JsContinue().also {
            context.loops[jump.loop]!!.continueStatements += it
        })
        return null
    }

    override fun visitThrow(expression: IrThrow, data: Unit): JsExpression? {
        expression.value.accept(this, Unit)?.let { context.addStatement(JsThrow(it)) }
        return null
    }

    override fun visitReturn(expression: IrReturn, data: Unit): JsExpression? {
        val jsResult = if (!KotlinBuiltIns.isUnit(expression.value.type)) expression.value.accept(this, Unit) else null
        context.addStatement(JsReturn(jsResult))
        return null
    }

    override fun visitTry(aTry: IrTry, data: Unit): JsExpression? {
        val jsTry = JsTry().apply { tryBlock = JsBlock() }

        return withTemporaryVar(aTry.type) { temporaryVar ->
            context.withStatements(jsTry.tryBlock.statements) {
                temporaryVar.translate(aTry.tryResult)
            }

            if (aTry.catches.isNotEmpty()) {
                val catchName = aTry.catches
                        .mapTo(mutableSetOf()) { it.parameter.name.identifier }
                        .singleOrNull()?.let { JsScope.declareTemporaryName(it) } ?: JsScope.declareTemporary()
                val jsCatch = JsCatch(catchName).apply { body = JsBlock() }
                jsTry.catches += jsCatch

                var statementConsumer: (JsStatement) -> Unit = { jsCatch.body.statements += it }
                for (irCatch in aTry.catches) {
                    val catchType = irCatch.parameter.type.constructor.declarationDescriptor as ClassDescriptor
                    val catchTypeRef = context.translateAsTypeReference(catchType)
                    val condition = buildJs { "Kotlin".dotPure("isType").invoke(catchName.refPure(), catchTypeRef).pure() }
                    val jsBody = context.withAliases(listOf(irCatch.catchParameter.descriptor to catchName.makeRef())) {
                        context.translateStatement {
                            temporaryVar.translate(irCatch.result)
                        }
                    }
                    val jsIf = JsIf(condition, jsBody)
                    statementConsumer(jsIf)
                    statementConsumer = { jsIf.elseStatement = it }
                }

                statementConsumer(JsThrow(catchName.makeRef()))
            }

            aTry.finallyExpression?.let { finallyExpression ->
                val finallyBlock = JsBlock().also { jsTry.finallyBlock = it }
                context.withStatements(finallyBlock.statements) {
                    temporaryVar.translate(finallyExpression)
                }
            }

            context.addStatement(jsTry)
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Unit): JsExpression? =
            expression.arguments
                    .map {
                        val jsExpr = it.accept(this, Unit)!!
                        if (!KotlinBuiltIns.isString(it.type)) {
                            buildJs { "Kotlin".dotPure("toString").invoke(jsExpr) }
                        }
                        else {
                            jsExpr
                        }
                    }
                    .reduce { acc, irExpression -> JsBinaryOperation(JsBinaryOperator.ADD, acc, irExpression) }

    override fun <T> visitConst(expression: IrConst<T>, data: Unit): JsExpression? {
        val value = expression.value
        return when (value) {
            is String -> JsStringLiteral(value)
            is Int -> JsIntLiteral(value)
            is Boolean -> JsBooleanLiteral(value)
            is Char -> JsIntLiteral(value.toInt())
            is Long -> buildJs { "Kotlin".dotPure("Long").newInstance(value.toInt().literal(), (value shr 32).toInt().literal()) }
            is Byte -> JsIntLiteral(value.toInt())
            is Short -> JsIntLiteral(value.toInt())
            is Float -> JsDoubleLiteral(value.toDouble())
            is Double -> JsDoubleLiteral(value)
            null -> JsNullLiteral()
            else -> null
        }
    }

    private fun withTemporaryVar(type: KotlinType, action: (TemporaryVar) -> Unit): JsExpression? {
        val tmpVarName = if (!KotlinBuiltIns.isUnit(type)) {
            JsScope.declareTemporary().also { context.addStatement(JsVars(JsVars.JsVar(it))) }
        }
        else {
            null
        }
        var tmpVarUsed = false

        val tmpVar = object : TemporaryVar {
            override fun translate(expression: IrExpression) {
                val result = expression.accept(this@IrExpressionTranslationVisitor, Unit)
                if (tmpVarName != null) {
                    if (result != null) {
                        tmpVarUsed = true
                        context.addStatement(buildJs {
                            statement(tmpVarName.makeRef().assign(result))
                        })
                    }
                }
                else {
                    if (result != null) {
                        context.addStatement(JsExpressionStatement(result))
                    }
                }
            }
        }

        action(tmpVar)
        return if (tmpVarUsed) tmpVarName?.makeRef() else null
    }

    interface TemporaryVar {
        fun translate(expression: IrExpression)
    }
}