package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.Ir
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import java.io.*

fun loadMetadata(configuration: CompilerConfiguration, file: File): ModuleDescriptorImpl {

    val reader = MetadataReader(file)

    var metadataAsString: String? = null
    var moduleName: String? = null
    val currentAbiVersion = configuration.get(KonanConfigKeys.ABI_VERSION)

    reader.use {
        val (nodeCount, kmetadataNodeArg) = reader.namedMetadataNode("kmetadata")

        if (nodeCount != 1) {
            throw Error("Unknown metadata format. The 'kmetadata' node has ${nodeCount} arguments. Don't know what to do.")
        }

        val operands = reader.metadataNodeOperands(kmetadataNodeArg)

        val abiNode = operands[0]
        val nameNode = operands[1]
        val dataNode = operands[2]

        val abiVersion = reader.string(abiNode).toInt()

        if (abiVersion != currentAbiVersion) {
            throw Error("Expected ABI version ${currentAbiVersion}, but the binary is ${abiVersion}")
        }
        moduleName = reader.string(nameNode)

        metadataAsString = reader.string(dataNode)
    }

    val moduleDescriptor = 
        deserializeModule(configuration, metadataAsString!!, moduleName!!)

    return moduleDescriptor
}

class MetadataReader(file: File) : Closeable {

    lateinit var llvmModule: LLVMModuleRef
    lateinit var llvmContext: LLVMContextRef

    init {
        memScoped {
            val bufRef = alloc<LLVMMemoryBufferRefVar>()
            val errorRef = allocPointerTo<CInt8Var>()
            val res = LLVMCreateMemoryBufferWithContentsOfFile(file.toString(), bufRef.ptr, errorRef.ptr)
            if (res != 0) {
                throw Error(errorRef.value?.asCString()?.toString())
            }

            llvmContext = LLVMContextCreate()!!
            val moduleRef = alloc<LLVMModuleRefVar>()
            val parseResult = LLVMParseBitcodeInContext2(llvmContext, bufRef.value, moduleRef.ptr)
            if (parseResult != 0) {
                throw Error(parseResult.toString())
            }

            llvmModule = moduleRef.value!!
        }
    }


    fun string(node: LLVMValueRef): String {
        memScoped { 
            val len = alloc<CInt32Var>()
            val str1 = LLVMGetMDString(node, len.ptr)!!
            val str = str1.asCString().toString() 
            return str

        }
    }

    fun namedMetadataNode(name: String): Pair<Int, LLVMValueRef> {
        memScoped {
            val nodeCount = LLVMGetNamedMetadataNumOperands(llvmModule, name)
            val nodeArray = allocArray<LLVMValueRefVar>(nodeCount)

            LLVMGetNamedMetadataOperands(llvmModule, "kmetadata", nodeArray[0].ptr)

            return Pair(nodeCount, nodeArray[0].value!!)
        }
    }

    fun metadataNodeOperands(metadataNode: LLVMValueRef): Array<LLVMValueRef> {
        memScoped {
            val operandCount = LLVMGetMDNodeNumOperands(metadataNode)
            val operandArray = allocArray<LLVMValueRefVar>(operandCount)

            LLVMGetMDNodeOperands(metadataNode, operandArray[0].ptr)

            return Array(operandCount, {index -> operandArray[index].value!!})
        }
    }

    override fun close() {
        LLVMDisposeModule(llvmModule)
        LLVMContextDispose(llvmContext)
    }
}

internal class MetadataGenerator(override val context: Context): ContextUtils {

    private fun metadataNode(args: List<LLVMValueRef?>): LLVMValueRef {
        memScoped {
            val references = allocArrayOf(args)
            return LLVMMDNode(references[0].ptr, args.size)!!
        }
    }

    private fun metadataFun(fn: LLVMValueRef, info: String): LLVMValueRef {
        val args = listOf(fn, metadataString(info));
        val md = metadataNode(args)
        return md
    }

    internal fun property(declaration: IrProperty) {
        if (declaration.backingField == null) return
        assert(declaration.backingField!!.descriptor == declaration.descriptor)
            
        context.ir.propertiesWithBackingFields.add(declaration.descriptor)
    }

    private fun emitModuleMetadata(name: String, md: LLVMValueRef?) {
        LLVMAddNamedMetadataOperand(context.llvmModule, name, md)
    }

    private fun metadataString(str: String): LLVMValueRef {
        return LLVMMDString(str, str.length)!!
    }

    internal fun endModule(module: IrModuleFragment) {
        val abiVersion = context.config.configuration.get(KonanConfigKeys.ABI_VERSION)
        val abiNode = metadataString("$abiVersion")

        val moduleName = metadataString(module.descriptor.name.asString())

        val serializer = KonanSerializationUtil(context)
        val moduleAsString = serializer.serializeModule(module.descriptor)
        val dataNode = metadataString(moduleAsString)

        val kmetadataArg  = metadataNode(listOf(abiNode, moduleName, dataNode))
        emitModuleMetadata("kmetadata", kmetadataArg)
    }
}

