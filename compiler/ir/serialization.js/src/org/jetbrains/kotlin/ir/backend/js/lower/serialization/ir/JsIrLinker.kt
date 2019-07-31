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

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, uniqId: UniqId) =
        moduleDescriptor.kotlinLibrary.irDeclaration(uniqId.index, uniqId.isLocal, fileIndex)

    override fun readSymbol(moduleDescriptor: ModuleDescriptor, fileIndex: Int, symbolIndex: Int) =
        moduleDescriptor.kotlinLibrary.symbol(symbolIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
        moduleDescriptor.kotlinLibrary.type(typeIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
        moduleDescriptor.kotlinLibrary.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
        moduleDescriptor.kotlinLibrary.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
        moduleDescriptor.kotlinLibrary.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
        moduleDescriptor.kotlinLibrary.fileCount()

    override fun List<IrFile>.handleClashes(uniqIdKey: UniqIdKey): IrFile {
        if (size == 1)
            return this[0]
        assert(size != 0)
        error("UniqId clash: ${uniqIdKey.uniqId.index}. Found in the " +
                      "[${this.joinToString { it.packageFragmentDescriptor.containingDeclaration.userName }}]")
    }

    private val ModuleDescriptor.userName get() = kotlinLibrary.libraryFile.absolutePath
}