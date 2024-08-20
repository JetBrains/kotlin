/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getAllArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class IrConstCheckerHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    override fun processModule(module: TestModule, info: IrBackendInput) {
        info.irModuleFragment.accept(IrConstChecker(), null)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

private class IrConstChecker : IrTypeTransformerVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        checkAnnotations(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        checkAnnotations(declaration)
        declaration.acceptChildrenVoid(this)
    }

    override fun <Type : IrType?> transformType(container: IrElement, type: Type): Type {
        if (type == null) return type

        checkAnnotations(type)
        if (type is IrSimpleType) {
            type.arguments.mapNotNull { it.typeOrNull }.forEach { transformType(container, it) }
        }
        return type
    }

    override fun visitField(declaration: IrField) {
        if (declaration.correspondingPropertySymbol?.owner?.isConst == true && declaration.initializer?.expression !is IrConst) {
            error("Const field is not containing const expression. Got ${declaration.initializer?.dump()}")
        }
        super.visitField(declaration)
    }

    private fun checkAnnotations(container: IrAnnotationContainer) {
        fun IrElement.isConst(): Boolean {
            return this is IrConst || this is IrGetEnumValue || this is IrClassReference || (this is IrConstructorCall && type.isAnnotation())
        }

        container.annotations.forEach { annotation ->
            fun IrElement.checkIsConst() {
                if (isConst()) return
                if (this is IrErrorExpression && this.description.startsWith("Stub expression")) return
                error("IR annotation has non constant argument.\n Annotation: ${annotation.dump()}.\n Argument: ${this.dump()}")
            }

            annotation.getAllArgumentsWithIr().forEach { (param, arg) ->
                val actualArg = arg ?: param.defaultValue?.expression
                when (actualArg) {
                    null -> error("IR annotation has null argument.\n Annotation: ${annotation.dump()}.")
                    is IrVararg -> actualArg.elements.forEach { element -> element.checkIsConst() }
                    else -> actualArg.checkIsConst()
                }
            }
        }
    }
}
