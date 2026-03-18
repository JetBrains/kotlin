/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.getWasmArrayAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.getWasmOpAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasWasmNoOpCastAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasWasmPrimitiveConstructorAnnotation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class IrFileToWasmIrGenerator(
    val typeGenerator: TypeGenerator,
    val declarationGenerator: DeclarationGenerator?,
    val importsGenerator: ImportsGenerator?,
    val backendContext: WasmBackendContext,
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        error("Unexpected element of type ${element::class}")
    }

    override fun visitProperty(declaration: IrProperty) {
        require(declaration.isExternal)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        // Type aliases are not material
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isExternal) return

        typeGenerator.generateClassTypes(declaration)

        if (declaration.getWasmArrayAnnotation() != null) return

        declarationGenerator?.generateClassDeclarations(declaration)
        importsGenerator?.generateClassImports(declaration)

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        // Constructor of inline class or with `@WasmPrimitiveConstructor` is empty
        if (declaration is IrConstructor &&
            (backendContext.inlineClassesUtils.isClassInlineLike(declaration.parentAsClass) || declaration.hasWasmPrimitiveConstructorAnnotation())
        ) {
            return
        }

        val isIntrinsic = declaration.hasWasmNoOpCastAnnotation() || declaration.getWasmOpAnnotation() != null
        if (isIntrinsic) {
            return
        }

        if (declaration.isFakeOverride)
            return

        typeGenerator.generateFunctionType(declaration)

        if (declaration is IrSimpleFunction && declaration.modality == Modality.ABSTRACT) {
            return
        }

        assert(declaration == declaration.realOverrideTarget) {
            "Sanity check that $declaration is a real function that can be used in calls"
        }

        declarationGenerator?.generateFunction(declaration)
        importsGenerator?.generateFunctionImport(declaration)
    }

    override fun visitField(declaration: IrField) {
        declarationGenerator?.generateField(declaration)
    }
}