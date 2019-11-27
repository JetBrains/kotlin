/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

val inventNamesForLocalClassesPhase = makeIrFilePhase<JvmBackendContext>(
    { context -> InventNamesForLocalClasses(context) },
    name = "InventNamesForLocalClasses",
    description = "Invent names for local classes and anonymous objects"
)

class InventNamesForLocalClasses(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(NameInventor(), Data(null, false))
    }

    /**
     * @property enclosingName JVM internal name of the enclosing class (including anonymous classes, local objects and callable references)
     * @property isLocal true if the next declaration to be encountered in the IR tree is local
     */
    private class Data(val enclosingName: String?, val isLocal: Boolean) {
        fun withName(newName: String): Data =
            Data(newName, isLocal)

        fun makeLocal(): Data =
            if (isLocal) this else Data(enclosingName, true)
    }

    private inner class NameInventor : IrElementVisitor<Unit, Data> {
        private val anonymousClassesCount = mutableMapOf<String, Int>()
        private val localFunctionNames = mutableMapOf<IrFunctionSymbol, String>()

        override fun visitClass(declaration: IrClass, data: Data) {
            if (!data.isLocal) {
                // This is not a local class, so we need not invent a name for it, the type mapper will correctly compute it
                // by navigating through its containers.

                val internalName = data.enclosingName?.let { enclosingName ->
                    "$enclosingName$${declaration.name.asString()}"
                } ?: (declaration.parent as IrFile).let { file ->
                    JvmClassName.byFqNameWithoutInnerClasses(file.fqName.child(declaration.name)).internalName
                }

                declaration.acceptChildren(this, data.withName(internalName))

                return
            }

            val internalName = inventName(declaration.name, data)
            context.putLocalClassType(declaration, Type.getObjectType(internalName))

            val newData = data.withName(internalName)

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

        override fun visitDeclaration(declaration: IrDeclaration, data: Data) {
            if (declaration !is IrDeclarationWithName) {
                declaration.acceptChildren(this, data)
                return
            }

            // We explicitly skip temporary variables (such as a for loop iterator, or a temporary value for an elvis operator)
            // because they are not present in the original source code and their names should not affect names of local entities.
            if (declaration.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR ||
                declaration.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            ) {
                declaration.acceptChildren(this, data)
                return
            }

            val enclosingName = data.enclosingName
            val simpleName = declaration.name.asString()

            val internalName = when {
                declaration.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA -> {
                    inventName(null, data).also { name ->
                        // We save the name of the lambda to reuse it in the reference to it (produced by the closure conversion) later.
                        localFunctionNames[(declaration as IrFunction).symbol] = name
                    }
                }
                declaration is IrFunction && declaration.parent !is IrClass -> {
                    // In the old backend, only names of non-local functions are stored in names of anonymous classes. All other names
                    // are replaced with indices. For example, a local class `L` in a top-level function `f` will have the name `...$f$L`,
                    // but inside a local function `g` (which is in `f`) it will have the name `...$f$1$L` (_not_ `...$f$g$L`).
                    inventName(null, data)
                }
                enclosingName != null -> "$enclosingName$$simpleName"
                else -> simpleName
            }

            val newData = data.withName(internalName).makeLocal()
            if (declaration is IrProperty && declaration.isDelegated) {
                // Old backend currently reserves a name here, in case a property reference-like anonymous object will need
                // to be generated in the codegen later, which is now happening for local delegated properties in inline functions.
                // See CodegenAnnotatingVisitor.visitProperty and ExpressionCodegen.initializePropertyMetadata.
                inventName(null, newData)
            }

            declaration.acceptChildren(this, newData)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: Data) {
            val internalName = localFunctionNames[expression.symbol] ?: inventName(null, data)
            context.putLocalClassType(expression, Type.getObjectType(internalName))

            expression.acceptChildren(this, data)
        }

        override fun visitPropertyReference(expression: IrPropertyReference, data: Data) {
            val internalName = inventName(null, data)
            context.putLocalClassType(expression, Type.getObjectType(internalName))

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
            check(enclosingName != null) { "There should be at least one name in the stack for every local declaration that needs a name" }

            val simpleName = if (sourceName == null || sourceName.isSpecial) {
                val count = (anonymousClassesCount[enclosingName] ?: 0) + 1
                anonymousClassesCount[enclosingName] = count
                count.toString()
            } else {
                sourceName
            }

            return JvmCodegenUtil.sanitizeNameIfNeeded("$enclosingName$$simpleName", context.state.languageVersionSettings)
        }
    }
}
