/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.ir.declarations.*

class JsMapping : Mapping() {

    // Wasm mappings
    val wasmNestedExternalToNewTopLevelFunction: DeclarationMapping<IrFunction, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val wasmExternalObjectToGetInstanceFunction: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val wasmExternalClassToInstanceCheck: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val wasmGetJsClass: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
}
