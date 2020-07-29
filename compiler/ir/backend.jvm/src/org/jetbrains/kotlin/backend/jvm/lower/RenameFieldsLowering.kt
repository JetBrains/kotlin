/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
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
            // We never rename fields that are part of public ABI (backing fields of const, lateinit, and JvmField properties).
            // Therefore we consider such fields first, in order to make sure it'll claim its original name.
            // If there are non-static and static (moved from companion) fields with the same name, we try to make static properties retain
            // their original names first, since this is what the old JVM backend did. However this can easily be changed without any
            // major binary compatibility consequences (modulo access to private members via Java reflection).
            for (field in fields.sortedBy {
                when {
                    it.isPublicAbi() -> 0
                    it.isStatic -> 1
                    else -> 2
                }
            }) {
                val oldName = field.name
                val newName = if (count == 0) oldName else Name.identifier(oldName.asString() + "$$count")
                count++

                if (field.isPublicAbi()) continue

                newNames[field] = newName
            }
        }

        val renamer = FieldRenamer(newNames)
        irFile.transform(renamer, null)

        irFile.transform(FieldAccessTransformer(renamer.newSymbols), null)
    }

    private fun IrField.isPublicAbi(): Boolean {
        if (!visibility.isPublicAPI) return false
        val correspondingProperty = correspondingPropertySymbol?.owner
        return isJvmField ||
                origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE ||
                correspondingProperty != null && (correspondingProperty.isLateinit || correspondingProperty.isConst)
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
        return declaration.factory.buildField {
            updateFrom(declaration)
            name = newName
        }.also {
            it.parent = declaration.parent
            it.initializer = declaration.initializer
                ?.transform(this, null)
                ?.patchDeclarationParents(it)

            newSymbols[declaration] = it.symbol
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
