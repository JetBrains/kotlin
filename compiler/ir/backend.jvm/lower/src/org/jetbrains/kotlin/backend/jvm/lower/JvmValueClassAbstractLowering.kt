/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.MemoizedValueClassAbstractReplacements
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal abstract class JvmValueClassAbstractLowering(val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {
    abstract val replacements: MemoizedValueClassAbstractReplacements

    final override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    abstract fun IrClass.isSpecificLoweringLogicApplicable(): Boolean

    abstract fun IrFunction.isFieldGetterToRemove(): Boolean

    abstract override fun visitClassNew(declaration: IrClass): IrStatement

    abstract fun handleSpecificNewClass(declaration: IrClass)

    protected fun transformFunctionFlat(function: IrFunction): List<IrDeclaration>? {
        if (function is IrConstructor && function.isPrimary && function.constructedClass.isSpecificLoweringLogicApplicable()) {
            return null
        }

        val replacement = replacements.getReplacementFunction(function)
        
        if (replacement == null) {
            if (function is IrConstructor) {
                val constructorReplacement = replacements.getReplacementRegularClassConstructor(function)
                if (constructorReplacement != null) {
                    addBindingsFor(function, constructorReplacement)
                    return transformFlattenedConstructor(function, constructorReplacement)
                }
            }
            function.transformChildrenVoid()
            // Non-mangled functions can override mangled functions under some conditions, e.g., a function
            // `fun f(): Nothing` can override a function `fun f(): UInt`. The former is not mangled, while
            // the latter is.
            //
            // This is a potential problem for bridge generation, where we have to ensure that the overridden
            // symbols are always up to date. Right now they might not be since we lower each file independently
            // and since deserialized declarations are not mangled at all.
            if (function is IrSimpleFunction) {
                function.overriddenSymbols = replacements.replaceOverriddenSymbols(function)
            }
            return null
        }

        if (function is IrSimpleFunction && function.overriddenSymbols.any { it.owner.parentAsClass.isFun }) {
            // If fun interface methods are already mangled, do not mangle them twice.
            val suffix = function.hashSuffix()
            if (suffix != null && function.name.asString().endsWith(suffix)) {
                function.transformChildrenVoid()
                return null
            }
        }

        addBindingsFor(function, replacement)
        return when (function) {
            is IrSimpleFunction -> transformSimpleFunctionFlat(function, replacement)
            is IrConstructor -> transformSecondaryConstructorFlat(function, replacement)
            else -> throw IllegalStateException()
        }
    }

    private fun transformFlattenedConstructor(function: IrConstructor, replacement: IrConstructor): List<IrDeclaration>? {
        replacement.valueParameters.forEach {
            it.transformChildrenVoid()
            it.defaultValue?.patchDeclarationParents(replacement)
        }
        allScopes.push(createScope(function))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        return listOf(replacement)
    }

    private fun IrFunction.hashSuffix(): String? = InlineClassAbi.hashSuffix(
        this,
        context.state.functionsWithInlineClassReturnTypesMangled,
        context.state.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
    )

    protected abstract fun transformSecondaryConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration>

    private fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.valueParameters.forEach {
            it.transformChildrenVoid()
            it.defaultValue?.patchDeclarationParents(replacement)
        }
        allScopes.push(createScope(function))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        replacement.copyAttributes(function)

        // Don't create a wrapper for functions which are only used in an unboxed context
        if (function.overriddenSymbols.isEmpty() || replacement.dispatchReceiverParameter != null)
            return listOf(replacement)

        val bridgeFunction = createBridgeFunction(function, replacement)

        return listOfNotNull(replacement, bridgeFunction)
    }

    final override fun visitReturn(expression: IrReturn): IrExpression {
        expression.returnTargetSymbol.owner.safeAs<IrFunction>()?.let { target ->
            val suffix = target.hashSuffix()
            if (suffix != null && target.name.asString().endsWith(suffix))
                return super.visitReturn(expression)

            replacements.getReplacementFunction(target)?.let {
                return context.createIrBuilder(it.symbol, expression.startOffset, expression.endOffset).irReturn(
                    expression.value.transform(this, null)
                )
            }
        }
        return super.visitReturn(expression)
    }

    protected open fun visitStatementContainer(container: IrStatementContainer) {
        container.statements.transformFlat { statement ->
            if (statement is IrFunction)
                transformFunctionFlat(statement)
            else
                listOf(statement.transformStatement(this))
        }
    }

    final override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        visitStatementContainer(expression)
        return expression
    }

    final override fun visitBlockBody(body: IrBlockBody): IrBody {
        visitStatementContainer(body)
        return body
    }

    // Anonymous initializers in inline classes are processed when building the primary constructor.
    final override fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer): IrStatement =
        if (declaration.parent.safeAs<IrClass>()?.isSpecificLoweringLogicApplicable() == true)
            declaration
        else
            super.visitAnonymousInitializerNew(declaration)

    protected abstract fun addBindingsFor(original: IrFunction, replacement: IrFunction)
    protected abstract fun createBridgeFunction(function: IrSimpleFunction, replacement: IrSimpleFunction): IrSimpleFunction?

    protected fun typedArgumentList(function: IrFunction, expression: IrMemberAccessExpression<*>) = listOfNotNull(
        function.dispatchReceiverParameter?.let { it to expression.dispatchReceiver },
        function.extensionReceiverParameter?.let { it to expression.extensionReceiver }
    ) + function.valueParameters.map { it to expression.getValueArgument(it.index) }
}