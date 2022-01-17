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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrAbstractVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

internal val renameFieldsPhase = makeIrFilePhase(
    ::RenameFieldsLowering,
    name = "RenameFields",
    description = "Rename private fields (including fields copied from companion object) to avoid JVM declaration clash"
)

private class RenameFieldsLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val fields = mutableListOf<IrField>()
        irFile.accept(FieldCollector, fields)
        fields.sortBy {
            when {
                // We never rename public ABI fields (public and protected visibility) since they are accessible from Java
                // even in cases when Kotlin code would prefer an accessor. (And in some cases, such as enum entries and const
                // fields, Kotlin references the fields directly too.) Therefore we consider such fields first, in order to make
                // sure it'll claim its original name. There can be multiple such fields, in which case they will cause a platform
                // declaration clash if they map to the same JVM type - nothing we can do about that.
                it.visibility.isPublicAPI -> 0
                // If there are non-public non-static and static (moved from companion) fields with the same name, we try to make
                // static properties retain their original names first, since this is what the old JVM backend did. However this
                // can easily be changed without any major binary compatibility consequences (ignoring Java reflection).
                it.isStatic -> 1
                else -> 2
            }
        }

        val newNames = mutableMapOf<IrField, Name>()
        val count = hashMapOf<Pair<IrDeclarationParent, Name>, Int>()
        for (field in fields) {
            val key = field.parent to field.name
            val index = count[key] ?: 0
            if (index != 0 && !field.visibility.isPublicAPI) {
                newNames[field] = Name.identifier("${field.name}$$index")
            }
            count[key] = index + 1
        }

        val renamer = FieldRenamer(newNames)
        irFile.transform(renamer, null)
        irFile.transform(FieldAccessTransformer(renamer.newSymbols), null)
    }
}

private object FieldCollector : IrAbstractVisitor<Unit, MutableList<IrField>>() {
    override fun visitElement(element: IrElement, data: MutableList<IrField>) {
        element.acceptChildren(this, data)
    }

    override fun visitField(declaration: IrField, data: MutableList<IrField>) {
        data.add(declaration)
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
