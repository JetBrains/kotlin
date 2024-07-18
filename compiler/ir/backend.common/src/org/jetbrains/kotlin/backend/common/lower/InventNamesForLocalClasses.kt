/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.getAdditionalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.ir.getNonDefaultAdditionalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.ir.getOriginalStatementsFromInlinedBlock
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import kotlin.collections.set

abstract class InventNamesForLocalClasses(private val generateNamesForRegeneratedObjects: Boolean = false) : FileLoweringPass {

    protected abstract fun computeTopLevelClassName(clazz: IrClass): String
    protected abstract fun sanitizeNameIfNeeded(name: String): String

    /** Makes it possible to do customizations for [IrClass] */
    protected open fun customizeNameInventorData(clazz: IrClass, data: NameBuilder): NameBuilder = data

    protected abstract fun putLocalClassName(declaration: IrAttributeContainer, localClassName: String)

    override fun lower(irFile: IrFile) {
        irFile.accept(NameInventor(), NameBuilder.EMPTY)
    }

    /**
     * @property isLocal true if the next declaration to be encountered in the IR tree is local
     */
    protected class NameBuilder(
        val parent: NameBuilder? = EMPTY,
        val currentName: String,
        val isLocal: Boolean = parent?.isLocal ?: false,
        val processingInlinedFunction: Boolean = parent?.processingInlinedFunction ?: false,
    ) {
        companion object {
            val EMPTY = NameBuilder(parent = null, currentName = "")
        }

        fun append(namePart: String): NameBuilder {
            return NameBuilder(parent = this, currentName = namePart)
        }

        fun copy(
            isLocal: Boolean = this.isLocal,
            processingInlinedFunction: Boolean = this.processingInlinedFunction,
        ): NameBuilder {
            return NameBuilder(
                parent = this.parent,
                currentName = this.currentName,
                isLocal = isLocal,
                processingInlinedFunction = processingInlinedFunction
            )
        }

        /**
         * Returns the internal name of the enclosing class (including anonymous classes, local objects and callable references)
         */
        fun build(): String {
            return getEnclosingName()
        }

        private fun getEnclosingName(): String {
            val enclosingName = generateSequence(this) { it.parent }
                .toList().dropLast(1).reversed()
                .joinToString("\$") { it.currentName }
            return enclosingName
        }
    }

    private inner class NameInventor : IrElementVisitor<Unit, NameBuilder> {
        private val anonymousClassesCount = mutableMapOf<String, Int>()
        private val localFunctionNames = mutableMapOf<IrFunctionSymbol, String>()

        override fun visitElement(element: IrElement, data: NameBuilder) {
            element.acceptChildren(this, data)
        }

        override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: NameBuilder) {
            if (!generateNamesForRegeneratedObjects) {
                return inlinedBlock.getNonDefaultAdditionalStatementsFromInlinedBlock().forEach { it.accept(this, data) }
            }

            if (!data.processingInlinedFunction && inlinedBlock.isFunctionInlining()) {
                inlinedBlock.getAdditionalStatementsFromInlinedBlock().forEach { it.accept(this, data) }

                val inlinedAt = inlinedBlock.inlineCall.symbol.owner.name
                val newData = data.append("\$inlined\$$inlinedAt").copy(isLocal = true, processingInlinedFunction = true)

                return inlinedBlock.getOriginalStatementsFromInlinedBlock().forEach { it.accept(this, newData) }
            }
            super.visitInlinedFunctionBlock(inlinedBlock, data)
        }

        override fun visitClass(declaration: IrClass, data: NameBuilder) {
            visitClassWithCustomizedData(declaration, customizeNameInventorData(declaration, data))
        }

        private fun visitClassWithCustomizedData(declaration: IrClass, data: NameBuilder) {
            if (!data.isLocal) {
                // This is not a local class, so we need not invent a name for it, the type mapper will correctly compute it
                // by navigating through its containers.
                val internalName = if (data.parent != null) declaration.name.asString() else computeTopLevelClassName(declaration)
                val newData = data.append(internalName)
                declaration.acceptChildren(this, newData)
                return
            }

            val newData = data.appendName(declaration)
            putLocalClassName(declaration, newData.buildAndSanitize())

            // Old backend doesn't add the anonymous object name to the stack when traversing its super constructor arguments.
            // E.g. a lambda in the super call of an object literal "foo$1" will get the name "foo$2", not "foo$1$1".
            val newDataForConstructor = if (declaration.isAnonymousObject) data else newData

            for (child in declaration.declarations) {
                child.accept(this, if (child is IrConstructor) newDataForConstructor else newData)
            }
        }

        override fun visitConstructor(declaration: IrConstructor, data: NameBuilder) {
            // Constructor is a special case because its name "<init>" doesn't participate when creating names for local classes inside.
            declaration.acceptChildren(this, data.copy(isLocal = true))
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: NameBuilder) {
            if (declaration !is IrDeclarationWithName ||
                // Skip temporary variables because they are not present in source code, and their names are not particularly
                // meaningful (e.g. `tmp$1`) in any case.
                declaration.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR ||
                declaration.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE ||
                // Skip variables storing delegates for local properties because we already have the name of the property itself.
                declaration.origin == IrDeclarationOrigin.PROPERTY_DELEGATE
            ) {
                declaration.acceptChildren(this, data)
                return
            }

            val simpleName = declaration.name.asString()

            val internalName = when {
                declaration is IrFunction && !NameUtils.hasName(declaration.name) -> {
                    // Replace "unnamed" function names with indices.
                    data.appendName(null).also {
                        // We save the name of the function to reuse it in the reference to it (produced by the closure conversion) later.
                        localFunctionNames[declaration.symbol] = it.buildAndSanitize()
                    }
                }

                declaration is IrVariable && generateNamesForRegeneratedObjects || data.processingInlinedFunction -> data.copy()
                data.parent != null -> data.appendName(declaration)
                else -> NameBuilder(currentName = simpleName)
            }

            val newData = internalName.copy(isLocal = true)
            if ((declaration is IrProperty && declaration.isDelegated) || declaration is IrLocalDelegatedProperty) {
                // Old backend currently reserves a name here, in case a property reference-like anonymous object will need
                // to be generated in the codegen later, which is now happening for local delegated properties in inline functions.
                // See CodegenAnnotatingVisitor.visitProperty and ExpressionCodegen.initializePropertyMetadata.
                newData.appendName(null)
            }

            declaration.acceptChildren(this, newData)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: NameBuilder) {
            if (data.processingInlinedFunction && expression.originalBeforeInline == null) {
                // skip IrFunctionReference from `singleArgumentInlineFunction`
                return
            }
            val internalName = localFunctionNames[expression.symbol] ?: data.appendName(null).buildAndSanitize()
            putLocalClassName(expression, internalName)

            expression.acceptChildren(this, data)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: NameBuilder) {
            expression.acceptChildren(this, data)
            val internalName = localFunctionNames[expression.function.symbol] ?: data.appendName(null).buildAndSanitize()
            putLocalClassName(expression, internalName)
        }

        override fun visitPropertyReference(expression: IrPropertyReference, data: NameBuilder) {
            val internalName = data.appendName(null).buildAndSanitize()
            putLocalClassName(expression, internalName)

            expression.acceptChildren(this, data)
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: NameBuilder) {
            // Although IrEnumEntry is an IrDeclaration, its name shouldn't be added to nameStack. This is because each IrEnumEntry has
            // an IrClass with the same name underneath it, and that class should obtain the name of the form "Enum$Entry",
            // not "Enum$Entry$Entry".
            declaration.acceptChildren(this, data.copy(isLocal = true))
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: NameBuilder) {
            // We skip value parameters when constructing names to replicate behavior of the old backend, but this can be safely changed.
            declaration.acceptChildren(this, data.copy(isLocal = true))
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: NameBuilder) {
            if (declaration.correspondingPropertySymbol != null) {
                // Skip adding property accessors to the name stack because the name of the property (which is a parent) is already there.
                declaration.acceptChildren(this, data.copy(isLocal = true))
                return
            }
            if (declaration.isSuspendNonLocal()) {
                // Suspend functions have a continuation, which is essentially a local class
                val newData = data.appendName(declaration)
                val internalName = newData.appendName(null).buildAndSanitize()
                putLocalClassName(declaration, internalName)
            }

            super.visitSimpleFunction(declaration, data)
        }

        override fun visitField(declaration: IrField, data: NameBuilder) {
            // Skip field name because the name of the property is already there.
            declaration.acceptChildren(this, data.copy(isLocal = true))
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: NameBuilder) {
            // IrAnonymousInitializer is not an IrDeclaration, so we need to manually make all its children aware that they're local
            // and might need new invented names.
            declaration.acceptChildren(this, data.copy(isLocal = true))
        }

        private fun IrDeclaration?.isSuspendNonLocal(): Boolean {
            return this is IrSimpleFunction && isSuspend && body != null && origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }

        private fun NameBuilder.appendName(declaration: IrDeclarationWithName?): NameBuilder {
            val name = declaration?.name
            val enclosingName = build()
            check(parent != null || declaration.isSuspendNonLocal()) {
                """
                    There should be at least one name in the stack for every local declaration that needs a name
                    Source name: $name
                    Enclosing name: $enclosingName
                """.trimIndent()
            }

            val simpleName = if (name == null || name.isSpecial) {
                val count = (anonymousClassesCount[enclosingName.toUpperCaseAsciiOnly()] ?: 0) + 1
                anonymousClassesCount[enclosingName.toUpperCaseAsciiOnly()] = count
                count.toString()
            } else {
                name.toString()
            }

            return append(simpleName)
        }

        private fun NameBuilder.buildAndSanitize(): String {
            val name = this.build()
            return sanitizeNameIfNeeded(name)
        }
    }
}
