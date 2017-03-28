package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.serialization.deserializeModule
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.Closeable
import java.io.File

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
                throw Error(errorRef.value?.toKString())
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
            val str = str1.toKString()
            return str

        }
    }

    fun namedMetadataNodes(name: String): Array<LLVMValueRef> {
        val result = mutableListOf<LLVMValueRef>()
        memScoped {
            val nodeCount = LLVMGetNamedMetadataNumOperands(llvmModule, name)
            val nodeArray = allocArray<LLVMValueRefVar>(nodeCount)

            LLVMGetNamedMetadataOperands(llvmModule, name, nodeArray)

            //return Pair(nodeCount, nodeArray[0].value!!)
            for (index in 0..nodeCount-1) {
                result.add(nodeArray[index].value!!)
            }
        }
        return result.toTypedArray<LLVMValueRef>()
    }

    fun namedMetadataNode(name: String): Pair<Int, LLVMValueRef> {
        memScoped {
            val nodeCount = LLVMGetNamedMetadataNumOperands(llvmModule, name)
            val nodeArray = allocArray<LLVMValueRefVar>(nodeCount)

            LLVMGetNamedMetadataOperands(llvmModule, name, nodeArray)

            return Pair(nodeCount, nodeArray[0].value!!)
        }
    }

    fun metadataNodeOperands(metadataNode: LLVMValueRef): Array<LLVMValueRef> {
        memScoped {
            val operandCount = LLVMGetMDNodeNumOperands(metadataNode)
            val operandArray = allocArray<LLVMValueRefVar>(operandCount)

            LLVMGetMDNodeOperands(metadataNode, operandArray)

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
        return LLVMMDNode(args.toCValues(), args.size)!!
    }

    private fun metadataFun(fn: LLVMValueRef, info: String): LLVMValueRef {
        val args = listOf(fn, metadataString(info));
        val md = metadataNode(args)
        return md
    }

    private fun emitModuleMetadata(name: String, md: LLVMValueRef?) {
        LLVMAddNamedMetadataOperand(context.llvmModule, name, md)
    }

    private fun metadataString(str: String): LLVMValueRef {
        return LLVMMDString(str, str.length)!!
    }

    private fun addLinkData(module: IrModuleFragment) {
        val abiVersion = context.config.configuration.get(KonanConfigKeys.ABI_VERSION)
        val abiNode = metadataString("$abiVersion")
        val moduleName = metadataString(module.descriptor.name.asString())
        val moduleAsString = context.serializedLinkData
        val dataNode = metadataString(moduleAsString)

        val kmetadataArg  = metadataNode(listOf(abiNode, moduleName, dataNode))
        emitModuleMetadata("kmetadata", kmetadataArg)
    }

    internal fun endModule(module: IrModuleFragment) {
        addLinkData(module)
    }
}

