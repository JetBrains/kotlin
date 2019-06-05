/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.kotlinLibrary
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

class JsIrLinker(
    currentModule: ModuleDescriptor,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable
) : KotlinIrLinker(logger, builtIns, symbolTable, emptyList(), null, 0x1_0000_0000L),
    DescriptorUniqIdAware by JsDescriptorUniqIdAware {

    private val FUNCTION_INDEX_START: Long = indexAfterKnownBuiltins

    override fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean) =
        builtIns.getPrimitiveTypeOrNullByDescriptor(symbol.descriptor, hasQuestionMark)

    override val descriptorReferenceDeserializer =
        JsDescriptorReferenceDeserializer(currentModule, builtIns, FUNCTION_INDEX_START)

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

    override fun declareForwardDeclarations() {
        // since for `knownBuiltIns` such as FunctionN it is possible to have unbound symbols after deserialization
        // reference them through out lazy symbol table
        with(symbolTable) {
            ArrayList(unboundClasses).forEach { lazyWrapper.referenceClass(it.descriptor) }
            ArrayList(unboundConstructors).forEach { lazyWrapper.referenceConstructor(it.descriptor) }
            ArrayList(unboundEnumEntries).forEach { lazyWrapper.referenceEnumEntry(it.descriptor) }
            ArrayList(unboundFields).forEach { lazyWrapper.referenceField(it.descriptor) }
            ArrayList(unboundSimpleFunctions).forEach { lazyWrapper.referenceSimpleFunction(it.descriptor) }
            ArrayList(unboundTypeParameters).forEach { lazyWrapper.referenceTypeParameter(it.descriptor) }
        }
    }
}