/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.setDeclarationsParent

data class LocationForLocalDeclarationExtraction(val newContainer: IrDeclarationParent, val extractedUnder: IrStatement?)

/**
 * Returns the nearest location where this local declaration can be lifted so that it's no longer local.
 */
fun IrDeclaration.locationForExtraction(): LocationForLocalDeclarationExtraction {
    var extractedUnder: IrStatement? = this
    var newContainer = parent
    while (newContainer is IrDeclaration && newContainer !is IrClass && newContainer !is IrScript) {
        extractedUnder = newContainer
        newContainer = newContainer.parent
    }
    when (newContainer) {
        is IrStatementContainer -> {
            // TODO: check if it is the correct behavior
            if (extractedUnder == this) {
                extractedUnder = (newContainer.statements.indexOf(extractedUnder) + 1)
                    .takeIf { it > 0 && it < newContainer.statements.size }
                    ?.let { newContainer.statements[it] }
            }
        }
        is IrDeclarationContainer -> {}
        else -> error("Unexpected container type $newContainer")
    }
    return LocationForLocalDeclarationExtraction(newContainer, extractedUnder)
}

fun IrDeclaration.moveTo(location: LocationForLocalDeclarationExtraction) {
    val (newContainer, extractedUnder) = location
    when (newContainer) {
        is IrStatementContainer -> {
            val insertIndex = extractedUnder?.let { newContainer.statements.indexOf(it) } ?: -1
            if (insertIndex >= 0) {
                newContainer.statements.add(insertIndex, this)
            } else {
                newContainer.statements.add(this)
            }
            setDeclarationsParent(newContainer)
        }
        is IrDeclarationContainer -> {
            newContainer.addChild(this)
        }
        else -> error("Unexpected container type $newContainer")
    }
}

//This lower takes part of old LocalDeclarationLowering job to pop up local classes from functions
open class LocalClassPopupLowering(
    val context: BackendContext,
) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    private data class ExtractedLocalClass(val local: IrClass, val location: LocationForLocalDeclarationExtraction)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val extractedLocalClasses = arrayListOf<ExtractedLocalClass>()

        irBody.transform(object : IrElementTransformerVoidWithContext() {

            override fun visitClassNew(declaration: IrClass): IrStatement {
                val currentScope =
                    if (allScopes.size > 1) allScopes[allScopes.lastIndex - 1] else createScope(container as IrSymbolOwner)
                if (!shouldPopUp(declaration, currentScope)) return declaration

                val locationForExtraction = container.locationForExtraction()
                extractedLocalClasses.add(ExtractedLocalClass(declaration, locationForExtraction))
                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            }
        }, null)

        for ((local, locationForExtraction) in extractedLocalClasses) {
            local.moveTo(locationForExtraction)
        }
    }

    protected open fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean =
        klass.isLocalNotInner()
}
