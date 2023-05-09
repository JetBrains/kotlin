/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentFunctionFactory
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name

class JsDefaultArgumentFunctionFactory(context: CommonBackendContext) : DefaultArgumentFunctionFactory(context) {

    override fun IrType.hasNullAsUndefinedValue() = false

    override fun IrFunction.generateDefaultArgumentStubFrom(original: IrFunction, useConstructorMarker: Boolean) {
        copyAttributesFrom(original)
        copyTypeParametersFrom(original)
        copyReturnTypeFrom(original)
        copyReceiversFrom(original)
        copyValueParametersFrom(original)

        if (!original.isTopLevel) {
            introduceContextParam()
        }
    }

    private fun IrFunction.introduceContextParam() = addValueParameter {
        name = Name.identifier("\$super")
        type = parentAsClass.defaultType.makeNullable()
        origin = JsLoweredDeclarationOrigin.JS_SUPER_CONTEXT_PARAMETER
    }

}