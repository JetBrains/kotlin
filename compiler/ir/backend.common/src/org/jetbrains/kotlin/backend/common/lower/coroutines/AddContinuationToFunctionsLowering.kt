/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.coroutines

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replaces suspend functions with regular non-suspend functions with additional
 * continuation parameter `$cont` of type [kotlin.coroutines.Continuation].
 *
 * Replaces return type with `Any?` or `Any` (for non-nullable types) to indicate that suspend
 * functions can return special values like [kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED]
 * which might not be a subtype of original return type.
 */

open class AddContinuationToNonLocalSuspendFunctionsLowering(override val context: CommonBackendContext) :
    SuspendFunctionsLoweringUtils, DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? =
        if (declaration is IrSimpleFunction && declaration.isSuspend) {
            listOf(transformSuspendFunction(declaration))
        } else {
            null
        }
}

/**
 * Similar to [AddContinuationToNonLocalSuspendFunctionsLowering] but processes local functions.
 * Useful for Kotlin/JS IR backend which keeps local declarations up until code generation.
 */
class AddContinuationToLocalSuspendFunctionsLowering(override val context: CommonBackendContext) :
    SuspendFunctionsLoweringUtils, BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                declaration.transformChildrenVoid()
                return if (declaration.isSuspend) {
                    transformSuspendFunction(declaration)
                } else {
                    declaration
                }
            }
        })
    }
}
