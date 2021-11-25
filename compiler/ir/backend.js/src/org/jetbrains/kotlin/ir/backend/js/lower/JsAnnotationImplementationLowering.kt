/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isPrimitiveArray


// JS PIR (and IC) requires DeclarationTransformer instead of FileLoweringPass
class JsAnnotationImplementationTransformer(val jsContext: JsIrBackendContext) :
    AnnotationImplementationTransformer(jsContext, null),
    DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? =
        if (declaration is IrClass && declaration.isAnnotationClass) listOf(visitClassNew(declaration) as IrClass)
        else null

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        // No-op
        return expression
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.isAnnotationClass) {
            context.irFactory.stageController.unrestrictDeclarationListsAccess {
                implementGeneratedFunctions(declaration, declaration)
            }
        }
        return super.visitClassNew(declaration)
    }

    private val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> =
        requireNotNull(jsContext.ir.symbols.arraysContentEquals) { "contentEquals symbols should be defined in JS IR context" }

    override fun getArrayContentEqualsSymbol(type: IrType) =
        when {
            type.isPrimitiveArray() -> arraysContentEquals[type]
            else -> arraysContentEquals.entries.singleOrNull { (k, _) -> k.isArray() }?.value
        } ?: compilationException("Can't find an Arrays.contentEquals method for array type", type)

    override fun implementAnnotationPropertiesAndConstructor(
        implClass: IrClass,
        annotationClass: IrClass,
        generatedConstructor: IrConstructor
    ) {
        compilationException(
            "Should not be called",
            implClass
        )
    }
}