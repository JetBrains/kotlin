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
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.deserializedIr
import org.jetbrains.kotlin.name.Name

fun createJvmFileFacadeClass(
    origin: IrDeclarationOrigin,
    name: Name,
    source: SourceElement,
    deserializeIr: (IrClass) -> Boolean,
) = IrFactoryImpl.createClass(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    origin = origin,
    symbol = IrClassSymbolImpl(),
    name = name,
    kind = ClassKind.CLASS,
    visibility = DescriptorVisibilities.PUBLIC,
    modality = Modality.FINAL,
    source = source,
).apply {
    this.deserializedIr = lazy { deserializeIr(this) }
}