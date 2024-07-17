/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.render

class KlibSyntheticAccessorGenerator(
    context: CommonBackendContext
) : SyntheticAccessorGenerator<CommonBackendContext>(context, addAccessorToParent = true) {

    companion object {
        const val TOP_LEVEL_FUNCTION_SUFFIX_MARKER = "t"
    }

    override fun accessorModality(parent: IrDeclarationParent) = Modality.FINAL
    override fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopes: List<ScopeWithIr>) = parent

    override fun AccessorNameBuilder.buildFunctionName(
        function: IrSimpleFunction,
        superQualifier: IrClassSymbol?,
        scopes: List<ScopeWithIr>,
    ) {
        contribute(function.name.asString())

        val parent = function.parent
        if (parent is IrPackageFragment) {
            // This is a top-level function. Include the sanitized .kt file name to avoid potential clashes.
            check(parent is IrFile) {
                "Unexpected type of package fragment for top-level function ${function.render()}: ${parent::class.java}, ${parent.render()}"
            }

            contribute(TOP_LEVEL_FUNCTION_SUFFIX_MARKER + parent.packagePartClassName)
        }
    }

    override fun AccessorNameBuilder.buildFieldGetterName(field: IrField, superQualifierSymbol: IrClassSymbol?) {
        contribute("<get-${field.name}>")
        contribute(PROPERTY_MARKER)
    }

    override fun AccessorNameBuilder.buildFieldSetterName(field: IrField, superQualifierSymbol: IrClassSymbol?) {
        contribute("<set-${field.name}>")
        contribute(PROPERTY_MARKER)
    }
}
