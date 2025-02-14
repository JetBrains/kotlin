/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationMemberGenerator
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.*

/**
 * Creates `equals`, `hashCode` and `toString` methods in annotation classes.
 * The original annotation class is used without creating a separate implementation.
 */
// JS PIR (and IC) requires DeclarationTransformer instead of FileLoweringPass
class JsCommonAnnotationImplementationTransformer(context: JsCommonBackendContext) :
    AnnotationImplementationTransformer(context, context.symbolTable, null),
    DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? =
        if (declaration is IrClass && declaration.isAnnotationClass) listOf(visitClassNew(declaration) as IrClass)
        else null

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        // No-op
        return expression
    }

    override fun chooseConstructor(implClass: IrClass, expression: IrConstructorCall): IrConstructor =
        compilationException("Should not be called", implClass)

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (!declaration.isAnnotationClass) return declaration

        val generator = AnnotationImplementationMemberGenerator(
            context, symbolTable, declaration,
            nameForToString = "@" + declaration.fqNameWhenAvailable!!.asString(),
            forbidDirectFieldAccess = forbidDirectFieldAccessInMethods
        ) { type, a, b ->
            generatedEquals(this, type, a, b)
        }

        var eqFun: IrSimpleFunction? = null
        var hcFun: IrSimpleFunction? = null
        var toStringFun: IrSimpleFunction? = null

        for (f in declaration.functions) {
            when {
                f.isEquals() -> eqFun = f
                f.isToString() -> toStringFun = f
                f.isHashCode() -> hcFun = f
            }
        }

        checkNotNull(eqFun) { "Annotation class has no equals method" }
        checkNotNull(hcFun) { "Annotation class has no equals method" }
        checkNotNull(toStringFun) { "Annotation class has no equals method" }

        eqFun.isFakeOverride = false
        hcFun.isFakeOverride = false
        toStringFun.isFakeOverride = false

        val dispatchReceiverType = declaration.defaultType
        eqFun.dispatchReceiverParameter?.type = dispatchReceiverType
        hcFun.dispatchReceiverParameter?.type = dispatchReceiverType
        toStringFun.dispatchReceiverParameter?.type = dispatchReceiverType

        generateFunctionBodies(declaration, declaration, eqFun, hcFun, toStringFun, generator)
        declaration.addConstructorBodyForCompatibility()
        return declaration
    }

    private val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> =
        requireNotNull(context.symbols.arraysContentEquals) { "contentEquals symbols should be defined in the context" }

    override fun getArrayContentEqualsSymbol(type: IrType) =
        when {
            type.isPrimitiveArray() || type.isUnsignedArray() -> arraysContentEquals[type]
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