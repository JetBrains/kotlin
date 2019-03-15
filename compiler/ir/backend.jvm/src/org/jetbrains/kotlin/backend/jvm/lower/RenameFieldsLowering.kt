/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

internal val renameFieldsPhase = makeIrFilePhase(
    ::RenameFieldsLowering,
    name = "RenameFields",
    description = "Rename private fields (including fields copied from companion object) to avoid JVM declaration clash"
)

private class RenameFieldsLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val collector = FieldNameCollector()
        irFile.acceptVoid(collector)

        val newNames = mutableMapOf<IrField, Name>()
        for ((_, fields) in collector.nameToField) {
            if (fields.size < 2) continue

            var count = 0
            // We never rename JvmField properties, since they are public ABI. Therefore we consider the JvmField-annotated property first,
            // in order to make sure it'll claim its original name
            // TODO: also do not rename const properties
            for (field in fields.sortedByDescending { it.isJvmField }) {
                val oldName = field.name
                val newName = if (count == 0) oldName else Name.identifier(oldName.asString() + "$$count")
                count++

                // TODO: check visibility instead of annotation
                if (field.isJvmField) continue

                newNames[field] = newName
            }
        }

        val renamer = FieldRenamer(newNames)
        irFile.transform(renamer, null)

        irFile.transform(FieldAccessTransformer(renamer.newSymbols), null)
    }

    private val IrField.isJvmField: Boolean
        get() = hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
}

private class FieldNameCollector : IrElementVisitorVoid {
    val nameToField = mutableMapOf<Pair<IrDeclarationParent, Name>, MutableList<IrField>>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitField(declaration: IrField) {
        nameToField.getOrPut(declaration.parent to declaration.name) { mutableListOf() }.add(declaration)
    }
}

private class FieldRenamer(private val newNames: Map<IrField, Name>) : IrElementTransformerVoid() {
    val newSymbols = mutableMapOf<IrField, IrFieldSymbol>()

    override fun visitField(declaration: IrField): IrStatement {
        val newName = newNames[declaration] ?: return super.visitField(declaration)

        val descriptor = WrappedFieldDescriptor()
        val symbol = IrFieldSymbolImpl(descriptor)
        return IrFieldImpl(
            declaration.startOffset, declaration.endOffset, declaration.origin, symbol, newName,
            declaration.type, declaration.visibility, declaration.isFinal, declaration.isExternal, declaration.isStatic
        ).also {
            descriptor.bind(it)
            it.parent = declaration.parent
            it.initializer = declaration.initializer?.transform(this, null)
            it.metadata = declaration.metadata

            newSymbols[declaration] = symbol
        }
    }
}

private class FieldAccessTransformer(private val oldToNew: Map<IrField, IrFieldSymbol>) : IrElementTransformerVoid() {
    override fun visitGetField(expression: IrGetField): IrExpression {
        val newSymbol = oldToNew[expression.symbol.owner] ?: return super.visitGetField(expression)

        return IrGetFieldImpl(
            expression.startOffset, expression.endOffset, newSymbol, expression.type,
            expression.receiver?.transform(this, null),
            expression.origin, expression.superQualifierSymbol
        )
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val newSymbol = oldToNew[expression.symbol.owner] ?: return super.visitSetField(expression)

        return IrSetFieldImpl(
            expression.startOffset, expression.endOffset, newSymbol,
            expression.receiver?.transform(this, null),
            expression.value.transform(this, null),
            expression.type,
            expression.origin, expression.superQualifierSymbol
        )
    }
}
