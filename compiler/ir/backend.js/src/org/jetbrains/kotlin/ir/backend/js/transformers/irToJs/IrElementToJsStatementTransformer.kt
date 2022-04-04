/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.isTheLastReturnStatementIn
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToJsStatementTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

    override fun visitFunction(declaration: IrFunction, data: JsGenerationContext) = JsEmpty.also {
        assert(declaration.origin == JsIrBackendContext.callableClosureOrigin) {
            "The only possible Function Declaration is one composed in Callable Reference Lowering"
        }
    }

    override fun visitBlockBody(body: IrBlockBody, context: JsGenerationContext): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, context) })
    }

    override fun visitBlock(expression: IrBlock, context: JsGenerationContext): JsStatement {
        val newContext = (expression as? IrReturnableBlock)?.inlineFunctionSymbol?.let {
            context.newFile(it.owner.file, context.currentFunction, context.localNames)
        } ?: context

        val statements = expression.statements.map { it.accept(this, newContext) }

        return if (expression is IrReturnableBlock) {
            val label = context.getNameForReturnableBlock(expression)
            val wrappedStatements = statements.wrapInCommentsInlineFunctionCall(expression)

            if (label != null) {
                JsLabel(label, JsBlock(wrappedStatements))
            } else {
                JsCompositeBlock(wrappedStatements)
            }
        } else {
            JsBlock(statements)
        }
    }

    private fun List<JsStatement>.wrapInCommentsInlineFunctionCall(expression: IrReturnableBlock): List<JsStatement> {
        val inlineFunctionSymbol = expression.inlineFunctionSymbol ?: return this
        val correspondingProperty = (inlineFunctionSymbol.owner as? IrSimpleFunction)?.correspondingPropertySymbol
        val owner = correspondingProperty?.owner ?: inlineFunctionSymbol.owner
        val funName = owner.fqNameWhenAvailable ?: owner.name
        return listOf(JsSingleLineComment(" Inline function '$funName' call")) + this
    }

    override fun visitComposite(expression: IrComposite, context: JsGenerationContext): JsStatement {
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitExpression(expression: IrExpression, context: JsGenerationContext): JsStatement {
        return expression.accept(IrElementToJsExpressionTransformer(), context).makeStmt()
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, context: JsGenerationContext): JsStatement {
        // If IrFunctionExpression is not used (i. e. the function expression is also a statement),
        // the generated function cannot be anonymous, so we don't erase its name, unlike in IrElementToJsExpressionTransformer
        return expression.function.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitBreak(jump: IrBreak, context: JsGenerationContext): JsStatement {
        return JsBreak(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) }).withSource(jump, context)
    }

    override fun visitContinue(jump: IrContinue, context: JsGenerationContext): JsStatement {
        return JsContinue(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) }).withSource(jump, context)
    }

    private fun IrExpression.maybeOptimizeIntoSwitch(context: JsGenerationContext, transformer: (JsExpression) -> JsStatement): JsStatement {
        if (this is IrWhen) {
            val stmtTransformer = { stmt: JsStatement ->
                assert(stmt is JsExpressionStatement) { "${render()} is not a statement $stmt" }
                transformer((stmt as JsExpressionStatement).expression)
            }
            SwitchOptimizer(context, isExpression = true, stmtTransformer).tryOptimize(this)?.let { return it }
        }

        return transformer(accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsStatement {
        val fieldName = context.getNameForField(expression.symbol.owner)
        val expressionTransformer = IrElementToJsExpressionTransformer()
        val dest = JsNameRef(fieldName, expression.receiver?.accept(expressionTransformer, context))
        return expression.value.maybeOptimizeIntoSwitch(context) { jsAssignment(dest, it).withSource(expression, context).makeStmt() }
    }

    override fun visitSetValue(expression: IrSetValue, context: JsGenerationContext): JsStatement {
        val owner = expression.symbol.owner
        val ref = JsNameRef(context.getNameForValueDeclaration(owner))
        return expression.value.maybeOptimizeIntoSwitch(context) { jsAssignment(ref, it).withSource(expression, context).makeStmt() }
    }

    override fun visitReturn(expression: IrReturn, context: JsGenerationContext): JsStatement {
        val targetSymbol = expression.returnTargetSymbol
        val lastStatementTransformer: (JsExpression) -> JsStatement =
            if (targetSymbol is IrReturnableBlockSymbol) {
                // TODO assert that value is Unit?
                {
                    context.getNameForReturnableBlock(targetSymbol.owner)
                    .takeIf { !expression.isTheLastReturnStatementIn(targetSymbol) }
                    ?.run { JsBreak(makeRef()) } ?: JsEmpty
                }
            } else {
                { JsReturn(it) }
            }

        return expression.value.maybeOptimizeIntoSwitch(context, lastStatementTransformer).withSource(expression, context)
    }

    override fun visitThrow(expression: IrThrow, context: JsGenerationContext): JsStatement {
        return expression.value.maybeOptimizeIntoSwitch(context) { JsThrow(it) }.withSource(expression, context)
    }

    override fun visitVariable(declaration: IrVariable, context: JsGenerationContext): JsStatement {
        val varName = context.getNameForValueDeclaration(declaration)
        val value = declaration.initializer

        if (value is IrWhen) {
            val varRef = varName.makeRef()
            val transformer = { stmt: JsStatement ->
                val expr = (stmt as JsExpressionStatement).expression
                JsBinaryOperation(JsBinaryOperator.ASG, varRef, expr).makeStmt()
            }

            SwitchOptimizer(context, isExpression = true, transformer).tryOptimize(value)?.let {
                return JsBlock(JsVars(JsVars.JsVar(varName)), it).withSource(declaration, context)
            }
        }

        return jsVar(varName, value, context).withSource(declaration, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsStatement {
        if (expression.symbol.owner.constructedClassType.isAny()) {
            return JsEmpty
        }
        return expression.accept(IrElementToJsExpressionTransformer(), context).makeStmt()
    }

    override fun visitCall(expression: IrCall, data: JsGenerationContext): JsStatement {
        if (expression.symbol.isUnitInstanceFunction(data)) {
            return JsEmpty
        }
        if (data.checkIfJsCode(expression.symbol) || data.checkIfAnnotatedWithJsFunc(expression.symbol)) {
            return JsCallTransformer(expression, data).generateStatement()
        }
        return translateCall(expression, data, IrElementToJsExpressionTransformer()).withSource(expression, data).makeStmt()
    }

    private fun IrFunctionSymbol.isUnitInstanceFunction(context: JsGenerationContext): Boolean {
        return owner.origin === JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION &&
                owner.returnType.classifierOrNull === context.staticContext.backendContext.irBuiltIns.unitClass
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, context: JsGenerationContext): JsStatement {

        // TODO: implement
        return JsEmpty
    }

    override fun visitTry(aTry: IrTry, context: JsGenerationContext): JsStatement {

        val jsTryBlock = aTry.tryResult.accept(this, context).asBlock()

        val jsCatch = aTry.catches.singleOrNull()?.let {
            val name = context.getNameForValueDeclaration(it.catchParameter)
            val jsCatchBlock = it.result.accept(this, context)
            JsCatch(emptyScope, name.ident, jsCatchBlock)
        }

        val jsFinallyBlock = aTry.finallyExpression?.accept(this, context)?.asBlock()

        return JsTry(jsTryBlock, jsCatch, jsFinallyBlock)
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsStatement {
        return SwitchOptimizer(context).tryOptimize(expression) ?: expression.toJsNode(this, context, ::JsIf) ?: JsEmpty
    }

    override fun visitWhileLoop(loop: IrWhileLoop, context: JsGenerationContext): JsStatement {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        val loopStatement = JsWhile(loop.condition.accept(IrElementToJsExpressionTransformer(), context),
                                    loop.body?.accept(this, context) ?: JsEmpty)
        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, context: JsGenerationContext): JsStatement {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        val loopStatement =
            JsDoWhile(loop.condition.accept(IrElementToJsExpressionTransformer(), context), loop.body?.accept(this, context) ?: JsEmpty)
        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }
}

