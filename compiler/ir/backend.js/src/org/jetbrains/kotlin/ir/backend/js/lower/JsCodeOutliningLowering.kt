/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.syntheticBodyIsNotSupported
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIntrinsics
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.FunctionWithJsFuncAnnotationInliner
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.translateJsCodeIntoStatementList
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentDeclarationsWithSelf
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

/**
 * Outlines `kotlin.js.js(code: String)` calls where the JavaScript code passed as a string literal references Kotlin locals.
 * Makes the usages of locals explicit.
 *
 * Transforms a call to `kotlin.js.js` into a call to generated local external function annotated with `@kotlin.js.JsOutlinedFunction`.
 *
 * The `sourceMap` argument of the annotation maps the offsets in its `code` argument to the offsets in the original Kotlin source.
 *
 * **Before the transformation:**
 * ```kotlin
 * fun foo(x: Int): Int {
 *   val theAnswer = 42
 *   return js("x + theAnswer")
 * }
 * ```
 *
 * **After the transformation:**
 * ```kotlin
 * fun foo(x: Int): Int {
 *
 *   @kotlin.js.JsOutlinedFunction(
 *     code = "function (x, theAnswer) { return x + theAnswer; }",
 *     sourceMap = """
 *     {
 *       "version": 3
 *       "sources": ["foo.kt"]
 *       "sourcesContent": [null]
 *       "names":[],
 *       "mappings": "+BAKc,CAAE,CAAE,S"
 *     }
 *     """
 *   )
 *   /*local*/ external fun foo$outlinedJsCode$(x: Int, theAnswer: Int): dynamic
 *
 *   val theAnswer = 42
 *   return foo$outlinedJsCode$(x, theAnswer)
 * }
 * ```
 *
 * The outlined functions are inlined again later by [FunctionWithJsFuncAnnotationInliner] during the codegen phase.
 */
@PhaseDescription("JsCodeOutliningLowering")
class JsCodeOutliningLowering(
    val loweringContext: LoweringContext,
    val intrinsics: JsIntrinsics,
    val dynamicType: IrDynamicType,
) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // Fast path to avoid tracking locals scopes for bodies without js() calls
        if (!irBody.containsCallsTo(intrinsics.jsCode))
            return

        val replacer = JsCodeOutlineTransformer(loweringContext, intrinsics, dynamicType, container)
        irBody.transformChildrenVoid(replacer)

        val outlinedFunctions = replacer.outlinedFunctions
        if (outlinedFunctions.isEmpty()) return
        putOutlinedFunctionsIntoContainer(outlinedFunctions, container, irBody)
    }

    private fun putOutlinedFunctionsIntoContainer(outlinedFunctions: List<IrFunction>, container: IrDeclaration, irBody: IrBody) {
        // The only possible containers are: IrAnonymousInitializer, IrFunction, IrEnumEntry, IrField.
        // These are the ones that are `IrDeclaration` and have an `IrBody`.

        for (outlinedFunction in outlinedFunctions) {
            outlinedFunction.parent = container.parentDeclarationsWithSelf.firstIsInstanceOrNull<IrDeclarationParent>()
                ?: compilationException("Unexpected container to insert the outlined function to", container)
        }

        when (irBody) {
            is IrBlockBody -> irBody.statements.addAll(0, outlinedFunctions)
            is IrExpressionBody -> {
                val builder = loweringContext.createIrBuilder(container.symbol)
                irBody.expression = builder.irBlock(irBody.startOffset, irBody.endOffset) {
                    +outlinedFunctions
                    +irBody.expression
                }
            }
            is IrSyntheticBody -> syntheticBodyIsNotSupported(container)
        }
    }

    companion object {
        val OUTLINED_JS_CODE_ORIGIN = IrDeclarationOriginImpl("OUTLINED_JS_CODE")
    }
}

private fun IrElement.containsCallsTo(symbol: IrFunctionSymbol): Boolean {
    var result = false
    acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (result) return
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (expression.symbol == symbol) {
                result = true
                return
            }
            super.visitCall(expression)
        }
    })

    return result
}

private class JsCodeOutlineTransformer(
    val loweringContext: LoweringContext,
    val intrinsics: JsIntrinsics,
    val dynamicType: IrDynamicType,
    val container: IrDeclaration,
) : IrElementTransformerVoid() {
    val outlinedFunctions = mutableListOf<IrFunction>()

    val localScopes: MutableList<HashMap<String, IrValueDeclaration>> =
        mutableListOf(hashMapOf())

    init {
        if (container is IrFunction) {
            container.valueParameters.forEach {
                registerValueDeclaration(it)
            }
        }
    }

    inline fun <T> withLocalScope(body: () -> T): T {
        localScopes.push(hashMapOf())
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

    override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        return super.visitValueParameter(declaration).also { registerValueDeclaration(declaration) }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return super.visitVariable(declaration).also { registerValueDeclaration(declaration) }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return outlineJsCodeIfNeeded(expression) ?: super.visitCall(expression)
    }

    fun outlineJsCodeIfNeeded(expression: IrCall): IrExpression? {
        if (expression.symbol != intrinsics.jsCode)
            return null

        val jsCodeArg = expression.getValueArgument(0) ?: compilationException("Expected js code string", expression)
        val jsStatements = translateJsCodeIntoStatementList(jsCodeArg, container) ?: return null

        // Collect used Kotlin local variables and parameters.
        val scope = JsScopesCollector().apply { acceptList(jsStatements) }
        val localsUsageCollector = KotlinLocalsUsageCollector(scope, ::findValueDeclarationWithName).apply { acceptList(jsStatements) }
        val kotlinLocalsUsedInJs = localsUsageCollector.usedLocals

        if (kotlinLocalsUsedInJs.isEmpty())
            return null

        // Building outlined IR function skeleton
        val outlinedFunction = createOutlinedFunction(kotlinLocalsUsedInJs)
        outlinedFunctions += outlinedFunction
        val annotation = addSpecialAnnotation(outlinedFunction)

        // Building JS Ast function
        val newFun = createJsFunction(jsStatements, kotlinLocalsUsedInJs)
        val (jsFunCode, sourceMap) = printJsCodeWithDebugInfo(newFun)
        annotation.putValueArgument(0, jsFunCode.toIrConst(loweringContext.irBuiltIns.stringType))
        annotation.putValueArgument(1, sourceMap.toIrConst(loweringContext.irBuiltIns.stringType))

        return with(loweringContext.createIrBuilder(container.symbol)) {
            irCall(outlinedFunction).apply {
                kotlinLocalsUsedInJs.values.forEachIndexed { index, local ->
                    putValueArgument(index, irGet(local))
                }
            }
        }
    }

    private fun addSpecialAnnotation(outlinedFunction: IrSimpleFunction): IrConstructorCall {
        val builder = loweringContext.createIrBuilder(outlinedFunction.symbol)
        val annotation = builder.irCallConstructor(
            intrinsics.jsOutlinedFunctionAnnotationSymbol.constructors.first(),
            typeArguments = emptyList(),
        )
        outlinedFunction.annotations += annotation
        return annotation
    }

    private fun createJsFunction(jsStatements: List<JsStatement>, kotlinLocalsUsedInJs: Map<JsName, IrValueDeclaration>): JsFunction {
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
        return newFun
    }

    private fun printJsCodeWithDebugInfo(jsFunction: JsFunction): Pair<String, String> {
        val jsCode = TextOutputImpl()
        val sourceMapBuilder = SourceMap3Builder(
            generatedFile = null,
            getCurrentOutputColumn = jsCode::getColumn,
            pathPrefix = "",
        )
        val sourceMapBuilderConsumer = SourceMapBuilderConsumer(
            File("."),
            sourceMapBuilder,
            SourceFilePathResolver(emptyList()),
            provideExternalModuleContent = false,
        )
        JsToStringGenerationVisitor(jsCode, sourceMapBuilderConsumer).accept(jsFunction)
        return jsCode.toString() to sourceMapBuilder.build()
    }

    private fun createOutlinedFunction(kotlinLocalsUsedInJs: Map<JsName, IrValueDeclaration>): IrSimpleFunction {
        val outlinedFunction = loweringContext.irFactory.buildFun {
            val containerName = (container as? IrDeclarationWithName)?.name?.asString()
            name = Name.identifier(containerName?.let { "$it\$outlinedJsCode\$" } ?: "outlinedJsCode\$")
            returnType = dynamicType
            visibility = DescriptorVisibilities.LOCAL
            isExternal = true
            origin = JsCodeOutliningLowering.OUTLINED_JS_CODE_ORIGIN
        }
        // We don't need this function's body. Using empty block body stub, because some code might expect all functions to have bodies.
        outlinedFunction.body = loweringContext.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        kotlinLocalsUsedInJs.values.forEach { local ->
            outlinedFunction.addValueParameter {
                name = local.name
                type = local.type
            }
        }

        return outlinedFunction
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
    private val findValueDeclarationWithName: (String) -> IrValueDeclaration?,
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
