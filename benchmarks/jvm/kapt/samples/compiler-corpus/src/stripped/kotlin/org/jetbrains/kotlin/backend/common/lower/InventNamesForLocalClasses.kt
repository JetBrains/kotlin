/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.getOriginalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.ir.getTmpVariablesForArguments
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

/**
 * Invents names for local classes and anonymous objects.
 */
abstract class InventNamesForLocalClasses(private val shouldIncludeVariableName: Boolean = true) : FileLoweringPass {

    protected abstract fun computeTopLevelClassName(clazz: IrClass): String
    protected abstract fun sanitizeNameIfNeeded(name: String): String

    /** Makes it possible to do customizations for [IrClass] */
    protected open fun customizeNameInventorData(clazz: IrClass, data: NameBuilder): NameBuilder = TODO()

    protected abstract fun putLocalClassName(declaration: IrElement, localClassName: String)

    override fun lower(irFile: IrFile) { TODO() }

    /**
     * @property isLocal true if the next declaration to be encountered in the IR tree is local
     * @property processingInlinedFunction true if inside scope of `IrInlinedFunctionBlock`
     * @property nameBelongToVariable true if `currentName` is the name of a variable
     */
    protected class NameBuilder(
        val parent: NameBuilder? = EMPTY,
        val currentName: String,
        val isLocal: Boolean = parent?.isLocal ?: false,
        val processingInlinedFunction: Boolean = parent?.processingInlinedFunction ?: false,
        private val nameBelongToVariable: Boolean = false
    ) {
        companion object {
            val EMPTY = NameBuilder(parent = null, currentName = "")
        }

        fun append(namePart: String): NameBuilder { TODO() }

        fun copy(
            isLocal: Boolean = this.isLocal,
            processingInlinedFunction: Boolean = this.processingInlinedFunction,
            nameBelongToVariable: Boolean = this.nameBelongToVariable,
        ): NameBuilder { TODO() }

        /**
         * Returns the internal name of the enclosing class (including anonymous classes, local objects and callable references)
         */
        fun build(): String { TODO() }

        private fun getEnclosingName(): String { TODO() }
    }

    private inner class NameInventor : IrVisitor<Unit, NameBuilder>() {
        private val anonymousClassesCount = mutableMapOf<String, Int>()
        private val localFunctionNames = mutableMapOf<IrFunctionSymbol, String>()

        override fun visitElement(element: IrElement, data: NameBuilder) { TODO() }

        override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: NameBuilder) { TODO() }

        override fun visitClass(declaration: IrClass, data: NameBuilder) { TODO() }

        private fun visitClassWithCustomizedData(declaration: IrClass, data: NameBuilder) { TODO() }

        override fun visitConstructor(declaration: IrConstructor, data: NameBuilder) { TODO() }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: NameBuilder) { TODO() }

        override fun visitVariable(declaration: IrVariable, data: NameBuilder) { TODO() }

        override fun visitFunctionReference(expression: IrFunctionReference, data: NameBuilder) { TODO() }

        override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: NameBuilder) { TODO() }

        override fun visitRichPropertyReference(expression: IrRichPropertyReference, data: NameBuilder) { TODO() }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: NameBuilder) { TODO() }

        override fun visitPropertyReference(expression: IrPropertyReference, data: NameBuilder) { TODO() }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: NameBuilder) { TODO() }

        override fun visitValueParameter(declaration: IrValueParameter, data: NameBuilder) { TODO() }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: NameBuilder) { TODO() }

        override fun visitField(declaration: IrField, data: NameBuilder) { TODO() }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: NameBuilder) { TODO() }

        private fun IrDeclaration?.isSuspendNonLocal(): Boolean { TODO() }

        private fun NameBuilder.appendName(declaration: IrDeclarationWithName?): NameBuilder { TODO() }

        private fun NameBuilder.buildAndSanitize(): String { TODO() }
    }
}
