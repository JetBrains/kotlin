/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering.DeclarationKeyForScope
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering.DeclarationKeyForScope.Companion.createDeclarationKeyForScopeBySanitizedName
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
 * An attribute to store an invented (container-wise unique) name for a local function.
 *
 * Note: We store the new name for the local function in the [inventedNameForLocalFunction] attribute
 * till the moment the function is regenerated in [LocalDeclarationsLowering], where the copy of the function
 * gets the new name. This is done for a reason: We need to maintain the clear
 * 'original_function_symbol_after_Fir2Ir_with_original_name' -> 'lowered_function_with_new_name_and_owner'
 * mapping in [org.jetbrains.kotlin.backend.jvm.JvmEvaluatorData.localDeclarationsData.transformedDeclarations]
 * to keep "expression evaluation" feature working in JVM IDE.
 */
internal var IrSimpleFunction.inventedNameForLocalFunction: Name? by irAttribute(copyByDefault = false)

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

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    private val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
    private val declarationScopesWithCounter: MutableMap<IrClass, MutableMap<DeclarationKeyForScope, ScopeWithCounter>> = mutableMapOf()

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

        override fun visitElement(element: IrElement, data: Data) { TODO() }

        override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: Data) { TODO() }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: Data) { TODO() }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Data) { TODO() }

        override fun visitClass(declaration: IrClass, data: Data) { TODO() }

        private fun generateNameForLiftedFunction(
            function: IrSimpleFunction,
            newOwnerForLiftedUpFunction: IrDeclarationParent,
        ): Name { TODO() }

        private fun suggestLocalName(declaration: IrDeclarationWithName): String { TODO() }

        // Need to keep LocalFunctionContext.index
        private fun IrSymbolOwner.getOrCreateScopeWithCounter(): ScopeWithCounter = TODO()

        private fun IrField.getOrCreateScopeWithCounter(): ScopeWithCounter? { TODO() }

        private fun IrFunction.getOrCreateScopeWithCounter(): ScopeWithCounter? { TODO() }

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
        fun withCurrentClass(currentClass: IrClass): Data = TODO()

        fun withCurrentFunction(currentFunction: IrFunction): Data = TODO()

        fun withInline(isInline: Boolean): Data = TODO()
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
    override val suggestUniqueNames: Boolean = true,
) : InventNamesForLocalFunctions() {
    override val compatibilityModeForInlinedLocalDelegatedPropertyAccessors get() = false
    override fun sanitizeNameIfNeeded(name: String) = name
}
