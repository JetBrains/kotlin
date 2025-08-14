/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.parentsWithSelf
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.Name
import kotlin.collections.set

/**
 * Invent names for local functions before lifting them up.
 *
 * @property suggestUniqueNames When `true` appends a `$#index` suffix to lifted declaration names.
 * @property compatibilityModeForInlinedLocalDelegatedPropertyAccessors Whether to keep old names for local delegated properties because of KT-49030.
 */
abstract class InventNamesForLocalFunctions : BodyLoweringPass {
    protected abstract val suggestUniqueNames: Boolean
    protected abstract val compatibilityModeForInlinedLocalDelegatedPropertyAccessors: Boolean

    protected abstract fun sanitizeNameIfNeeded(name: String): String

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.accept(
            NameInventor(container = container),
            Data(currentScope = null, isInInlineFunction = false, isInLambdaFunction = false)
        )
    }

    private val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
    private val declarationScopesWithCounter: MutableMap<IrClass, MutableMap<Name, ScopeWithCounter>> = mutableMapOf()

    private class LocalFunctionContext(
        val index: Int,
        val newOwnerForLiftedUpFunction: IrDeclarationContainer,
        val isNestedInLambda: Boolean,
    )

    private inner class NameInventor(
        val container: IrDeclaration
    ) : IrVisitor<Unit, Data>() {
        val enclosingClass: IrClass? by lazy { getEnclosing<IrClass>() }
        val enclosingField: IrField? by lazy { getEnclosing<IrField>().takeIf { it?.parentClassOrNull != null } }
        val enclosingFunction: IrFunction? by lazy { getEnclosing<IrFunction>().takeIf { it !is IrConstructor && it?.parentClassOrNull != null } }
        val enclosingPackageFragment: IrPackageFragment by lazy { container.getPackageFragment() }

        override fun visitElement(element: IrElement, data: Data) {
            element.acceptChildren(this, data)
        }

        override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: Data) {
            super.visitInlinedFunctionBlock(
                inlinedBlock,
                data.withInline(isInline = inlinedBlock.isFunctionInlining())
            )
        }

        override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: Data) {
            expression.boundValues.forEach { it.accept(this, data) }
            expression.invokeFunction.acceptChildren(this, data)
        }

        override fun visitRichPropertyReference(expression: IrRichPropertyReference, data: Data) {
            expression.boundValues.forEach { it.accept(this, data) }
            expression.getterFunction.acceptChildren(this, data)
            expression.setterFunction?.acceptChildren(this, data)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: Data) {
            expression.function.acceptChildren(this, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Data) {
            if (declaration.visibility == DescriptorVisibilities.LOCAL) {
                val enclosingScope = data.currentScope
                    ?: enclosingField?.getOrCreateScopeWithCounter()
                    ?: enclosingFunction?.getOrCreateScopeWithCounter()
                    ?: enclosingClass?.getOrCreateScopeWithCounter()
                    // File is required for K/N because file declarations are not split by classes.
                    ?: enclosingPackageFragment.getOrCreateScopeWithCounter()
                val index =
                    if (declaration.name.isSpecial || declaration.name in enclosingScope.usedLocalFunctionNames)
                        enclosingScope.counter++
                    else -1
                val newOwnerForLiftedUpFunction =
                    data.currentScope?.let {
                        when (it.irElement) {
                            is IrDeclarationContainer -> it.irElement
                            is IrField -> it.irElement.parentClassOrNull!!
                            is IrFunction -> localFunctions[enclosingScope.irElement]!!.newOwnerForLiftedUpFunction
                            else -> error("Unknown owner for lowered declaration")
                        }
                    }
                        ?: (enclosingScope.irElement as? IrField)?.let { enclosingField -> enclosingField.parentClassOrNull!! }
                        ?: (enclosingScope.irElement as? IrFunction)?.let { enclosingFunction -> enclosingFunction.parentClassOrNull!! }
                        ?: enclosingScope.irElement as IrDeclarationContainer

                val functionContext = LocalFunctionContext(
                    index = index,
                    newOwnerForLiftedUpFunction = newOwnerForLiftedUpFunction,
                    isNestedInLambda = data.isInLambdaFunction,
                )
                localFunctions[declaration] = functionContext
                enclosingScope.usedLocalFunctionNames.add(declaration.name)

                declaration.name = generateNameForLiftedFunction(
                    function = declaration,
                    newOwnerForLiftedUpFunction = functionContext.newOwnerForLiftedUpFunction
                )
            }

            val newData = data.withInline(isInline = declaration.isInline)
            super.visitSimpleFunction(
                declaration,
                if (declaration.isLambda) newData.withCurrentFunction(currentFunction = declaration) else newData
            )
        }

        override fun visitClass(declaration: IrClass, data: Data) {
            super.visitClass(declaration, data.withCurrentClass(currentClass = declaration))
        }

        private fun generateNameForLiftedFunction(
            function: IrSimpleFunction,
            newOwnerForLiftedUpFunction: IrDeclarationParent,
        ): Name {
            val parents = function.parentsWithSelf.takeWhile { it != newOwnerForLiftedUpFunction }.toList().reversed()
            val nameFromParents = parents.joinToString(separator = "$") { suggestLocalName(it as IrDeclarationWithName) }
            // Local functions declared in anonymous initializers have classes as their parents.
            // Such anonymous initializers, however, are inlined into the constructors delegating to super class constructor.
            // There can be local functions declared in local function in init blocks (and further),
            // but such functions would have proper "safe" names (outerLocalFun1$outerLocalFun2$...$localFun).
            return if (parents.size == 1 && function.parent is IrClass)
                Name.identifier("_init_\$$nameFromParents")
            else
                Name.identifier(nameFromParents)
        }

        private fun suggestLocalName(declaration: IrDeclarationWithName): String {
            val declarationName = sanitizeNameIfNeeded(declaration.name.asString())

            val functionContext = localFunctions[declaration] ?: return declarationName
            if (functionContext.index <= 0) return declarationName

            val baseName = when {
                declaration.name.isSpecial -> if (functionContext.isNestedInLambda) "" else "lambda"
                else -> declarationName
            }

            if (!suggestUniqueNames) return baseName

            val separator = when {
                compatibilityModeForInlinedLocalDelegatedPropertyAccessors &&
                        declaration.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR &&
                        container is IrFunction && container.isInline -> "-"
                baseName.isEmpty() -> ""
                else -> "$"
            }
            return "$baseName$separator${functionContext.index}"
        }

        // Need to keep LocalFunctionContext.index
        private fun IrSymbolOwner.getOrCreateScopeWithCounter(): ScopeWithCounter =
            scopeWithCounter ?: ScopeWithCounter(this).also { scopeWithCounter = it }

        private fun IrField.getOrCreateScopeWithCounter(): ScopeWithCounter? {
            val klass = parentClassOrNull ?: return null
            return declarationScopesWithCounter.getOrPut(klass, ::mutableMapOf)
                .getOrPut(this.name) { ScopeWithCounter(this) }
        }

        private fun IrFunction.getOrCreateScopeWithCounter(): ScopeWithCounter? {
            val klass = parentClassOrNull ?: return null
            return declarationScopesWithCounter.getOrPut(klass, ::mutableMapOf)
                .getOrPut(this.name) { ScopeWithCounter(this) }
        }

        private inline fun <reified T : IrElement> getEnclosing(): T? {
            var currentParent = container as? T ?: container.parent
            while (currentParent is IrDeclaration && currentParent !is T) {
                currentParent = currentParent.parent
            }

            return currentParent as? T
        }
    }

    private class Data(
        val currentScope: ScopeWithCounter?,
        val isInInlineFunction: Boolean,
        val isInLambdaFunction: Boolean,
    ) {
        fun withCurrentClass(currentClass: IrClass): Data =
            // Don't cache local declarations
            Data(
                currentScope = ScopeWithCounter(irElement = currentClass),
                isInInlineFunction = isInInlineFunction,
                isInLambdaFunction = false
            )

        fun withCurrentFunction(currentFunction: IrFunction): Data =
            Data(
                currentScope = ScopeWithCounter(irElement = currentFunction),
                isInInlineFunction = isInInlineFunction,
                isInLambdaFunction = currentFunction.isLambda
            )

        fun withInline(isInline: Boolean): Data =
            if (isInline && !isInInlineFunction)
                Data(
                    currentScope = currentScope,
                    isInInlineFunction = true,
                    isInLambdaFunction = false
                )
            else this
    }

    companion object {
        private val IrFunction.isLambda: Boolean
            get() =
                origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA || origin == IrDeclarationOrigin.INLINE_LAMBDA
    }
}

private var IrSymbolOwner.scopeWithCounter: ScopeWithCounter? by irAttribute(copyByDefault = false)

private class ScopeWithCounter(val irElement: IrElement) {
    // Continuous numbering across all declarations in the container.
    var counter: Int = 0
    val usedLocalFunctionNames: MutableSet<Name> = hashSetOf()
}

class KlibInventNamesForLocalFunctions(
    private val context: CommonBackendContext,
    override val suggestUniqueNames: Boolean = true,
) : InventNamesForLocalFunctions() {
    override val compatibilityModeForInlinedLocalDelegatedPropertyAccessors get() = false
    override fun sanitizeNameIfNeeded(name: String) = name
}
