/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.kotlinLibrary
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

class JsIrLinker(
    currentModule: ModuleDescriptor,
    mangler: KotlinMangler,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable
) : KotlinIrLinker(logger, builtIns, symbolTable, emptyList(), null, PUBLIC_LOCAL_UNIQ_ID_EDGE),
    DescriptorUniqIdAware by JsDescriptorUniqIdAware {

    override fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean) =
        builtIns.getPrimitiveTypeOrNullByDescriptor(symbol.descriptor, hasQuestionMark)

    override val descriptorReferenceDeserializer =
        JsDescriptorReferenceDeserializer(currentModule, mangler, builtIns)

    override fun reader(moduleDescriptor: ModuleDescriptor, uniqId: UniqId): ByteArray {
        return moduleDescriptor.kotlinLibrary.irDeclaration(uniqId.index, uniqId.isLocal)
    }

    override val ModuleDescriptor.irHeader: ByteArray?
        get() = this.kotlinLibrary.irHeader

    override fun readSymbol(moduleDescriptor: ModuleDescriptor, symbolIndex: Int) =
        moduleDescriptor.kotlinLibrary.symbol(symbolIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, typeIndex: Int) =
        moduleDescriptor.kotlinLibrary.type(typeIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, stringIndex: Int) =
        moduleDescriptor.kotlinLibrary.string(stringIndex)

    override fun List<IrFile>.handleClashes(uniqIdKey: UniqIdKey): IrFile {
        if (size == 1)
            return this[0]
        assert(size != 0)
        error("UniqId clash: ${uniqIdKey.uniqId.index}. Found in the " +
                      "[${this.joinToString { it.packageFragmentDescriptor.containingDeclaration.userName }}]")
    }

    private val ModuleDescriptor.userName get() = kotlinLibrary.libraryFile.absolutePath
}