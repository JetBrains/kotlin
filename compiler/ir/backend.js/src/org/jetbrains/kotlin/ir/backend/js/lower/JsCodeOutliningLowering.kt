/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.translateJsCodeIntoStatementList
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// Outlines `kotlin.js.js(code: String)` calls where JS code references Kotlin locals.
// Makes locals usages explicit.
class JsCodeOutliningLowering(val backendContext: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // Fast path to avoid tracking locals scopes for bodies without js() calls
        if (!irBody.containsCallsTo(backendContext.intrinsics.jsCode))
            return

        val replacer = JsCodeOutlineTransformer(backendContext, container)
        irBody.transformChildrenVoid(replacer)
    }

    companion object {
        val OUTLINED_JS_CODE_ORIGIN = IrDeclarationOriginImpl("OUTLINED_JS_CODE")
    }
}

private fun IrElement.containsCallsTo(symbol: IrFunctionSymbol): Boolean {
    var result = false
    acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (expression.symbol == symbol) {
                result = true
            }
            super.visitCall(expression)
        }
    })

    return result
}

private class JsCodeOutlineTransformer(
    val backendContext: JsIrBackendContext,
    val container: IrDeclaration,
) : IrElementTransformerVoidWithContext() {
    val localScopes: MutableList<MutableMap<String, IrValueDeclaration>> =
        mutableListOf(mutableMapOf())

    init {
        if (container is IrFunction) {
            container.valueParameters.forEach {
                registerValueDeclaration(it)
            }
        }
    }

    inline fun <T> withLocalScope(body: () -> T): T {
        localScopes.push(mutableMapOf())
        val res = body()
        localScopes.pop()
        return res
    }

    fun registerValueDeclaration(irValueDeclaration: IrValueDeclaration) {
        val name = irValueDeclaration.name
        if (!name.isSpecial) {
            val identifier = name.identifier
            val currentScope = localScopes.lastOrNull()
                ?: compilationException(
                    "Expecting a scope",
                    irValueDeclaration
                )
            currentScope[identifier] = irValueDeclaration
        }
    }

    fun findValueDeclarationWithName(name: String): IrValueDeclaration? {
        for (i in (localScopes.size - 1) downTo 0) {
            val scope = localScopes[i]
            return scope[name] ?: continue
        }
        return null
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        return withLocalScope { super.visitContainerExpression(expression) }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        return withLocalScope { super.visitDeclaration(declaration) }
    }

    override fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
        return super.visitValueParameterNew(declaration).also { registerValueDeclaration(declaration) }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return super.visitVariable(declaration).also { registerValueDeclaration(declaration) }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return outlineJsCodeIfNeeded(expression) ?: super.visitCall(expression)
    }

    fun outlineJsCodeIfNeeded(expression: IrCall): IrExpression? {
        if (expression.symbol != backendContext.intrinsics.jsCode)
            return null

        val jsCodeArg = expression.getValueArgument(0) ?: compilationException("Expected js code string", expression)
        val jsStatements = translateJsCodeIntoStatementList(jsCodeArg, backendContext, container) ?: return null

        // Collect used Kotlin local variables and parameters.
        val scope = JsScopesCollector().apply { acceptList(jsStatements) }
        val localsUsageCollector = KotlinLocalsUsageCollector(scope, ::findValueDeclarationWithName).apply { acceptList(jsStatements) }
        val kotlinLocalsUsedInJs = localsUsageCollector.usedLocals

        if (kotlinLocalsUsedInJs.isEmpty())
            return null

        // Building outlined IR function skeleton
        val outlinedFunction = backendContext.irFactory.buildFun {
            name = Name.identifier(container.safeAs<IrDeclarationWithName>()?.name?.asString()?.let { "$it\$outlinedJsCode\$" }
                                       ?: "outlinedJsCode\$")
            returnType = backendContext.dynamicType
            isExternal = true
            origin = JsCodeOutliningLowering.OUTLINED_JS_CODE_ORIGIN
        }
        // We don't need this function's body. Using empty block body stub, because some code might expect all functions to have bodies.
        outlinedFunction.body = backendContext.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        outlinedFunction.parent = container.file
        container.file.declarations.add(outlinedFunction)
        kotlinLocalsUsedInJs.values.forEach { local ->
            outlinedFunction.addValueParameter {
                name = local.name
                type = local.type
            }
        }

        // Building JS Ast function
        val lastStatement = jsStatements.findLast { it !is JsSingleLineComment && it !is JsMultiLineComment }
        val newStatements = jsStatements.toMutableList()
        when (lastStatement) {
            is JsReturn -> {
            }
            is JsExpressionStatement -> {
                newStatements[jsStatements.lastIndex] = JsReturn(lastStatement.expression)
            }
            else -> {
                newStatements += JsReturn(JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)))
            }
        }
        val newFun = JsFunction(emptyScope, JsBlock(newStatements), "Outlined js() call")
        kotlinLocalsUsedInJs.keys.forEach { jsName ->
            newFun.parameters.add(JsParameter(jsName))
        }

        backendContext.addOutlinedJsCode(outlinedFunction.symbol, newFun)

        return with(backendContext.createIrBuilder(container.symbol)) {
            irCall(outlinedFunction).apply {
                kotlinLocalsUsedInJs.values.forEachIndexed { index, local ->
                    putValueArgument(index, irGet(local))
                }
            }
        }
    }
}

class JsScopesCollector : RecursiveJsVisitor() {
    private val functionsStack = mutableListOf(Scope(null))
    private val functionalScopes = mutableMapOf<JsFunction?, Scope>(null to functionsStack.first())

    private class Scope(val parent: Scope?) {
        private val variables = hashSetOf<String>()

        fun add(variableName: String) {
            variables.add(variableName)
        }

        fun variableWithNameExists(variableName: String): Boolean {
            return variables.contains(variableName) ||
                    parent?.variableWithNameExists(variableName) == true
        }
    }

    override fun visitVars(x: JsVars) {
        super.visitVars(x)
        val currentScope = functionsStack.last()
        x.vars.forEach { currentScope.add(it.name.ident) }
    }

    override fun visitFunction(x: JsFunction) {
        val parentScope = functionsStack.last()
        val newScope = Scope(parentScope).apply {
            val name = x.name?.ident
            if (name != null) add(name)
            x.parameters.forEach { add(it.name.ident) }
        }
        functionsStack.push(newScope)
        functionalScopes[x] = newScope
        super.visitFunction(x)
        functionsStack.pop()
    }

    fun varWithNameExistsInScopeOf(function: JsFunction?, variableName: String): Boolean {
        return functionalScopes[function]!!.variableWithNameExists(variableName)
    }
}

private class KotlinLocalsUsageCollector(
    private val scopeInfo: JsScopesCollector,
    private val findValueDeclarationWithName: (String) -> IrValueDeclaration?
) : RecursiveJsVisitor() {
    private val functionStack = mutableListOf<JsFunction?>(null)
    private val processedNames = mutableSetOf<String>()
    private val kotlinLocalsUsedInJs = mutableMapOf<JsName, IrValueDeclaration>()

    val usedLocals: Map<JsName, IrValueDeclaration>
        get() = kotlinLocalsUsedInJs

    override fun visitFunction(x: JsFunction) {
        functionStack.push(x)
        super.visitFunction(x)
        functionStack.pop()
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        super.visitNameRef(nameRef)
        val name = nameRef.name.takeIf { nameRef.qualifier == null } ?: return
        // With this approach we should be able to find all usages of Kotlin variables in JS code.
        // We will also collect shadowed usages, but it is OK since the same shadowing will be present in generated JS code.
        // Keeping track of processed names to avoid registering them multiple times
        if (processedNames.add(name.ident) && !name.isDeclaredInsideJsCode()) {
            findValueDeclarationWithName(name.ident)?.let {
                kotlinLocalsUsedInJs[name] = it
            }
        }
    }

    private fun JsName.isDeclaredInsideJsCode(): Boolean {
        return scopeInfo.varWithNameExistsInScopeOf(functionStack.peek(), ident)
    }
}
