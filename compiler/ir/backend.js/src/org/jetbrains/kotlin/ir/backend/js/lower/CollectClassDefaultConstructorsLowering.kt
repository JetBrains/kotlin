/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isClass

class CollectClassDefaultConstructorsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.couldContainDefaultConstructor()) {
            context.mapping.classToItsDefaultConstructor[declaration] = declaration.defaultConstructor ?: return null
        }

        return null
    }

    private fun IrClass.couldContainDefaultConstructor(): Boolean {
        return isClass && !isValue && !isExpect && modality != Modality.ABSTRACT && modality != Modality.SEALED
    }

    private val IrClass.defaultConstructor: IrConstructor?
        get() = constructors.singleOrNull { it.visibility == DescriptorVisibilities.PUBLIC && it.isDefaultConstructor() }

    private fun IrFunction.isDefaultConstructor(): Boolean {
        return valueParameters.isEmpty() || valueParameters.all { it.defaultValue != null }
    }
}