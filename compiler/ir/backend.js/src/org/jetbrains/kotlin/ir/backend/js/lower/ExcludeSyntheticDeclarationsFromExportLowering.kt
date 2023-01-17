/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.parentEnumClassOrNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor

class ExcludeSyntheticDeclarationsFromExportLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration.shouldBeExcludedFromExport()) {
            if (declaration is IrSimpleFunction) {
                declaration.correspondingPropertySymbol?.owner?.excludeFromJsExport()
            }

            declaration.excludeFromJsExport()
        }

        return null
    }

    private fun IrDeclaration.shouldBeExcludedFromExport(): Boolean {
        return isExportedSyntheticEnumEntriesProperty() || isComponentMethodOfDataClass()
    }

    private fun IrDeclaration.isComponentMethodOfDataClass(): Boolean {
        if (this !is IrSimpleFunction) return false
        val original = getOriginalFunction()
        val parent = original.parentClassOrNull ?: return false
        return parent.isExported(context) &&
                original.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER &&
                original.name.identifier.startsWith(StandardNames.DATA_CLASS_COMPONENT_PREFIX)
    }

    private fun IrDeclaration.isExportedSyntheticEnumEntriesProperty(): Boolean {
        return this is IrSimpleFunction &&
                parentEnumClassOrNull?.isExported(context) == true &&
                (body as? IrSyntheticBody)?.kind == IrSyntheticBodyKind.ENUM_ENTRIES
    }

    private fun IrDeclaration.excludeFromJsExport() {
        annotations += generateJsExportIgnoreCall()
    }

    private fun generateJsExportIgnoreCall(): IrConstructorCall {
        return JsIrBuilder.buildConstructorCall(context.intrinsics.jsExportIgnoreAnnotationSymbol.owner.primaryConstructor!!.symbol)
    }

    private fun IrSimpleFunction.getOriginalFunction(): IrSimpleFunction {
        return if (overriddenSymbols.isEmpty()) {
            this
        } else {
            overriddenSymbols.first().owner.getOriginalFunction()
        }
    }
}