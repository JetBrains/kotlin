/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.correspondingStatic
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.transformMemberToStaticFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isExportedInterface
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Extracts default implementations from exported interfaces
 */
class ExportedDefaultImplementationsLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return when (declaration) {
            is IrSimpleFunction -> declaration.transformExportedDefaultImplementation()
                ?.let { listOf(declaration, it) }

            is IrProperty -> listOf(declaration.apply {
                this.getter = this.getter?.let(::transformAccessor)
                this.setter = this.setter?.let(::transformAccessor)
            })
            else -> null
        }
    }

    private fun transformAccessor(accessor: IrSimpleFunction) =
        accessor.transformExportedDefaultImplementation() ?: accessor

    private fun IrSimpleFunction.transformExportedDefaultImplementation(): IrSimpleFunction? =
        runIf(
            origin != CONVERTERS_TO_JS_COLLECTIONS &&
            modality != Modality.ABSTRACT
                    && overriddenSymbols.isEmpty()
                    && dispatchReceiverParameter != null
                    && parentClassOrNull?.isExportedInterface(context) == true
        ) {
            with(context) {
                val staticFunction = transformMemberToStaticFunction(this@transformExportedDefaultImplementation)
                body = null
                correspondingStatic = staticFunction
                staticFunction
            }
        }
}
