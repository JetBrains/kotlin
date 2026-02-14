/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature

enum class WasmServiceImportExportKind(val prefix: String) {
    VTABLE($$"__vt$"),
    ITABLE($$"__it$"),
    RTTI($$"$__rt$"),
    FUNC($$"__fn$")
}

abstract class WasmCodegenContext(private val idSignatureRetriever: IdSignatureRetriever) {
    protected fun IrSymbol.getReferenceKey(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner as IrDeclaration)!!
}
