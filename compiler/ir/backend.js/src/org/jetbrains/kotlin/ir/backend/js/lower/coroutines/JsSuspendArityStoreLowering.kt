/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

class JsSuspendArityStoreLowering(context: JsIrBackendContext) : DeclarationTransformer {

    private var IrClass.suspendArityStore by context.mapping.suspendArityStore

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrClass) return null

        declaration.declarations
            .filterIsInstanceAnd<IrSimpleFunction> { it.isSuspend }
            .let { declaration.suspendArityStore = it }

        return null
    }
}