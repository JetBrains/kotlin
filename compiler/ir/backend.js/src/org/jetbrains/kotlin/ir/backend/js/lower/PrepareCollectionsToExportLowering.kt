/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

// TODO: Remove the lowering and move annotations into stdlib after solving problem with tests on KLIB
class PrepareCollectionsToExportLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val jsNameCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.intrinsics.jsNameAnnotationSymbol.primaryConstructorSymbol
    }
    private val jsExportIgnoreCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.intrinsics.jsExportIgnoreAnnotationSymbol.primaryConstructorSymbol
    }
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.intrinsics.jsImplicitExportAnnotationSymbol.primaryConstructorSymbol
    }

    private val IrClassSymbol.primaryConstructorSymbol: IrConstructorSymbol get() = owner.primaryConstructor!!.symbol

    private val exportedMethodNames = setOf(
        "asJsReadonlyArrayView",
        "asJsArrayView",
        "asJsReadonlySetView",
        "asJsSetView",
        "asJsReadonlyMapView",
        "asJsMapView"
    )

    private val exportableSymbols = setOf(
        context.ir.symbols.list,
        context.ir.symbols.mutableList,
        context.ir.symbols.set,
        context.ir.symbols.mutableSet,
        context.ir.symbols.map,
        context.ir.symbols.mutableMap,
    )

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.symbol in exportableSymbols) {
            declaration.addJsName()
            declaration.markWithJsImplicitExport()

            declaration.declarations.forEach {
                if (it !is IrDeclarationWithName || it.name.toString() !in exportedMethodNames) {
                    it.excludeFromJsExport()
                }
            }
        }

        return null
    }

    private fun IrDeclaration.excludeFromJsExport() {
        if (this is IrSimpleFunction) {
            correspondingPropertySymbol?.owner?.excludeFromJsExport()
        }
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsExportIgnoreCtor)
    }

    private fun IrDeclarationWithName.addJsName() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsNameCtor).apply {
            putValueArgument(0, "Kt${name.asString()}".toIrConst(context.irBuiltIns.stringType))
        }
    }

    private fun IrDeclaration.markWithJsImplicitExport() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsImplicitExportCtor).apply {
            putValueArgument(0, true.toIrConst(context.irBuiltIns.booleanType))
        }
    }
}