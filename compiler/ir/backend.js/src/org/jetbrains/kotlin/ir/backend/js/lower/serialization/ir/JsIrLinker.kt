package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.library.CombinedIrFileReader
import org.jetbrains.kotlin.backend.common.library.DeclarationId
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.backend.common.serialization.knownBuiltins
import org.jetbrains.kotlin.ir.declarations.IrFunction
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
    KotlinIrLinker(currentModule, logger, builtIns, symbolTable, null),
    DescriptorUniqIdAware by JsDescriptorUniqIdAware {

    private val FUNCTION_INDEX_START: Long
    val moduleToReaderMap = mutableMapOf<ModuleDescriptor, CombinedIrFileReader>()

    init {
        // TODO: think about order
        var currentIndex = 0x1_0000_0000L
        builtIns.knownBuiltins.forEach {
            require(it is IrFunction)
            deserializedSymbols.put(UniqIdKey(null, UniqId(currentIndex, isLocal = false)), it.symbol)
            assert(symbolTable.referenceSimpleFunction(it.descriptor) == it.symbol)
            currentIndex++
        }

        FUNCTION_INDEX_START = currentIndex
    }

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
}