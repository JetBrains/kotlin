/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.util.IdSignature

interface IrProviderForCEnumAndCStructStubsBase {
    fun isCEnumOrCStruct(declarationDescriptor: DeclarationDescriptor): Boolean
    fun getDeclaration(
        descriptor: DeclarationDescriptor,
        idSignature: IdSignature,
        file: IrFile,
        symbolKind: BinarySymbolData.SymbolKind,
    ): IrSymbolOwner
}
