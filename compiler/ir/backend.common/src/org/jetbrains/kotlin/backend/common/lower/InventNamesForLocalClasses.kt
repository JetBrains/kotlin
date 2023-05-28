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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import kotlin.collections.set

abstract class InventNamesForLocalClasses(
    private val allowTopLevelCallables: Boolean,
    private val generateNamesForRegeneratedObjects: Boolean = false
) : FileLoweringPass {

    protected abstract fun computeTopLevelClassName(clazz: IrClass): String
    protected abstract fun sanitizeNameIfNeeded(name: String): String

    protected abstract fun putLocalClassName(declaration: IrAttributeContainer, localClassName: String)

    override fun lower(irFile: IrFile) {
        irFile.accept(NameInventor(), Data(null, false))
    }

    /**
     * @property enclosingName internal name of the enclosing class (including anonymous classes, local objects and callable references)
     * @property isLocal true if the next declaration to be encountered in the IR tree is local
     */
    private data class Data(val enclosingName: String?, val isLocal: Boolean, val processingInlinedFunction: Boolean = false) {
        fun makeLocal(): Data = if (isLocal) this else copy(isLocal = true)
    }

    private inner class NameInventor : IrElementVisitor<Unit, Data> {
        private val anonymousClassesCount = mutableMapOf<String, Int>()
        private val localFunctionNames = mutableMapOf<IrFunctionSymbol, String>()

        override fun visitContainerExpression(expression: IrContainerExpression, data: Data) {
            if (expression is IrInlinedFunctionBlock && !generateNamesForRegeneratedObjects) {
                return expression.getNonDefaultAdditionalStatementsFromInlinedBlock().forEach { it.accept(this, data) }
            }

            if (!data.processingInlinedFunction && expression is IrInlinedFunctionBlock && expression.isFunctionInlining()) {
                expression.getAdditionalStatementsFromInlinedBlock().forEach { it.accept(this, data) }

                val inlinedAt = expression.inlineCall.symbol.owner.name.asString()
                val newData = data.copy(
                    enclosingName = data.enclosingName + "$\$inlined\$$inlinedAt", isLocal = true, processingInlinedFunction = true
                )
                expression.getOriginalStatementsFromInlinedBlock().forEach { it.accept(this, newData) }

                return
            }
            super.visitContainerExpression(expression, data)
        }

        override fun visitClass(declaration: IrClass, data: Data) {
            if (!data.isLocal) {
                // This is not a local class, so we need not invent a name for it, the type mapper will correctly compute it
                // by navigating through its containers.
                val enclosingName = data.enclosingName
                val internalName = if (enclosingName != null) {
                    "$enclosingName$${declaration.name.asString()}"
                } else {
                    computeTopLevelClassName(declaration)
                }
                declaration.acceptChildren(this, data.copy(enclosingName = internalName))
                return
            }

            val internalName = inventName(declaration.name, data)
            putLocalClassName(declaration, internalName)

            val newData = data.copy(enclosingName = internalName)

            // Old backend doesn't add the anonymous object name to the stack when traversing its super constructor arguments.
            // E.g. a lambda in the super call of an object literal "foo$1" will get the name "foo$2", not "foo$1$1".
            val newDataForConstructor =
                if (declaration.isAnonymousObject) data else newData

            for (child in declaration.declarations) {
                child.accept(this, if (child is IrConstructor) newDataForConstructor else newData)
            }
        }

        override fun visitConstructor(declaration: IrConstructor, data: Data) {
            // Constructor is a special case because its name "<init>" doesn't participate when creating names for local classes inside.
            declaration.acceptChildren(this, data.makeLocal())
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: Data) {
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

            val enclosingName = data.enclosingName
            val simpleName = declaration.name.asString()

            val internalName = when {
                declaration is IrFunction && !NameUtils.hasName(declaration.name) -> {
                    // Replace "unnamed" function names with indices.
                    inventName(null, data).also { name ->
                        // We save the name of the function to reuse it in the reference to it (produced by the closure conversion) later.
                        localFunctionNames[declaration.symbol] = name
                    }
                }

                declaration is IrVariable && generateNamesForRegeneratedObjects || data.processingInlinedFunction -> enclosingName
                enclosingName != null -> "$enclosingName$$simpleName"
                else -> simpleName
            }

            val newData = data.copy(enclosingName = internalName, isLocal = true)
            if ((declaration is IrProperty && declaration.isDelegated) || declaration is IrLocalDelegatedProperty) {
                // Old backend currently reserves a name here, in case a property reference-like anonymous object will need
                // to be generated in the codegen later, which is now happening for local delegated properties in inline functions.
                // See CodegenAnnotatingVisitor.visitProperty and ExpressionCodegen.initializePropertyMetadata.
                inventName(null, newData)
            }

            declaration.acceptChildren(this, newData)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: Data) {
            if (data.processingInlinedFunction && expression.originalBeforeInline == null) {
                // skip IrFunctionReference from `singleArgumentInlineFunction`
                return
            }
            val internalName = localFunctionNames[expression.symbol] ?: inventName(null, data)
            putLocalClassName(expression, internalName)

            expression.acceptChildren(this, data)
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: Data) {
            expression.acceptChildren(this, data)
            val internalName = localFunctionNames[expression.function.symbol] ?: inventName(null, data)
            putLocalClassName(expression, internalName)
        }

        override fun visitPropertyReference(expression: IrPropertyReference, data: Data) {
            val internalName = inventName(null, data)
            putLocalClassName(expression, internalName)

            expression.acceptChildren(this, data)
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: Data) {
            // Although IrEnumEntry is an IrDeclaration, its name shouldn't be added to nameStack. This is because each IrEnumEntry has
            // an IrClass with the same name underneath it, and that class should obtain the name of the form "Enum$Entry",
            // not "Enum$Entry$Entry".
            declaration.acceptChildren(this, data.makeLocal())
        }

        override fun visitValueParameter(declaration: IrValueParameter, data: Data) {
            // We skip value parameters when constructing names to replicate behavior of the old backend, but this can be safely changed.
            declaration.acceptChildren(this, data.makeLocal())
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Data) {
            if (declaration.correspondingPropertySymbol != null) {
                // Skip adding property accessors to the name stack because the name of the property (which is a parent) is already there.
                declaration.acceptChildren(this, data.makeLocal())
                return
            }
            if (declaration.isSuspend && declaration.body != null && declaration.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                // Suspend functions have a continuation, which is essentially a local class
                val newData = data.copy(
                    enclosingName = inventName(
                        declaration.name,
                        if (data.enclosingName == null && allowTopLevelCallables) data.copy(enclosingName = "") else data
                    )
                )
                val internalName = inventName(null, newData)
                putLocalClassName(declaration, internalName)
            }

            super.visitSimpleFunction(declaration, data)
        }

        override fun visitField(declaration: IrField, data: Data) {
            // Skip field name because the name of the property is already there.
            declaration.acceptChildren(this, data.makeLocal())
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Data) {
            // IrAnonymousInitializer is not an IrDeclaration, so we need to manually make all its children aware that they're local
            // and might need new invented names.
            declaration.acceptChildren(this, data.makeLocal())
        }

        override fun visitElement(element: IrElement, data: Data) {
            element.acceptChildren(this, data)
        }

        private fun inventName(sourceName: Name?, data: Data): String {
            val enclosingName = data.enclosingName
            check(enclosingName != null) {
                """
                    There should be at least one name in the stack for every local declaration that needs a name
                    Source name: $sourceName
                    Data: $data
                """.trimIndent()
            }

            val simpleName = if (sourceName == null || sourceName.isSpecial) {
                val count = (anonymousClassesCount[enclosingName.toUpperCaseAsciiOnly()] ?: 0) + 1
                anonymousClassesCount[enclosingName.toUpperCaseAsciiOnly()] = count
                count.toString()
            } else {
                sourceName
            }

            return sanitizeNameIfNeeded("$enclosingName$$simpleName")
        }
    }
}
