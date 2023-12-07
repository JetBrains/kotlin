/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.DeserializableClass
import org.jetbrains.kotlin.name.Name

@OptIn(IrImplementationDetail::class)
class JvmFileFacadeClass(
    origin: IrDeclarationOrigin,
    name: Name,
    source: SourceElement,
    private val deserializeIr: (IrClass) -> Boolean,
) : IrClassImpl(
    UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
    IrClassSymbolImpl(), name, ClassKind.CLASS, DescriptorVisibilities.PUBLIC, Modality.FINAL,
    source = source
), DeserializableClass {

    private var irLoaded: Boolean? = null

    override fun loadIr(): Boolean {
        return irLoaded ?: deserializeIr(this).also { irLoaded = it }
    }
}