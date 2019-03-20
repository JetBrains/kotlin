/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.library.CombinedIrFileReader
import org.jetbrains.kotlin.backend.common.library.DeclarationId
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import java.io.File

class JsIrLinker(
    currentModule: ModuleDescriptor,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable) :
    KotlinIrLinker(logger, builtIns, symbolTable, null, 0x1_0000_0000L),
    DescriptorUniqIdAware by JsDescriptorUniqIdAware {

    private val FUNCTION_INDEX_START: Long = indexAfterKnownBuiltins

    val moduleToReaderMap = mutableMapOf<ModuleDescriptor, CombinedIrFileReader>()

    override fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean) =
        builtIns.getPrimitiveTypeOrNullByDescriptor(symbol.descriptor, hasQuestionMark)

    override val descriptorReferenceDeserializer =
        JsDescriptorReferenceDeserializer(currentModule, builtIns, FUNCTION_INDEX_START)

    override fun reader(moduleDescriptor: ModuleDescriptor, uniqId: UniqId) =
            moduleToReaderMap[moduleDescriptor]!!.declarationBytes(DeclarationId(uniqId.index, uniqId.isLocal))

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        byteArray: ByteArray,
        klibLocation: File,
        deserializationStrategy: DeserializationStrategy
    ): IrModuleFragment {
        val irFile = File(klibLocation, "ir/irCombined.knd")
        moduleToReaderMap[moduleDescriptor] = CombinedIrFileReader(irFile)
        return deserializeIrModuleHeader(moduleDescriptor, byteArray, deserializationStrategy)
    }

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
