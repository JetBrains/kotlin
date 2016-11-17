package org.jetbrains.kotlin.backend.konan.llvm


import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.konan.llvm.BinaryLinkdata.*
import org.jetbrains.kotlin.types.KotlinType

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.js.*
import org.jetbrains.kotlin.utils.*
import java.io.*

fun readModuleMetadata(file: File): String {

    val reader = MetadataReader(file)
    var metadataString: String? = null

    try {
        val metadataNode = reader.namedMetadataNode("kmetadata", 0);
        val stringNode = reader.metadataOperand(metadataNode, 0)!!
        metadataString = reader.string(stringNode)
    } finally {
        reader.close()
    }

    return metadataString!!
}

class MetadataReader(file: File) {

    lateinit var llvmModule: LLVMOpaqueModule
    lateinit var llvmContext: LLVMOpaqueContext

    init {
        memScoped {
            val bufRef = alloc(LLVMOpaqueMemoryBuffer.ref)
            val errorRef = alloc(Int8Box.ref)
            val res = LLVMCreateMemoryBufferWithContentsOfFile(file.toString(), bufRef, errorRef)
            if (res != 0) {
                throw Error(errorRef.value?.asCString()?.toString())
            }

            llvmContext = LLVMContextCreate()!!
            val moduleRef = alloc(LLVMOpaqueModule.ref)
            val parseResult = LLVMParseBitcodeInContext2(llvmContext, bufRef.value, moduleRef)
            if (parseResult != 0) {
                throw Error(parseResult.toString())
            }

            llvmModule = moduleRef.value!!
        }
    }


    fun string(node: LLVMOpaqueValue): String {
        memScoped { 
            val len = alloc(Int32Box)
            val str1 = LLVMGetMDString(node, len)!!
            val str = str1.asCString().toString() 
            return str

        }
    }

    fun namedMetadataNode(name: String, index: Int): LLVMOpaqueValue {
        memScoped {
            val nodeCount = LLVMGetNamedMetadataNumOperands(llvmModule, "kmetadata")!!
            val nodeArray = alloc(array[nodeCount](Ref to LLVMOpaqueValue))

            LLVMGetNamedMetadataOperands(llvmModule, "kmetadata", nodeArray[0])!!

            return nodeArray[0].value!!
        }
    }

    fun metadataOperand(metadataNode: LLVMOpaqueValue, index: Int): LLVMOpaqueValue {
        memScoped {
            val operandCount = LLVMGetMDNodeNumOperands(metadataNode)!!
            val operandArray = alloc(array[operandCount](Ref to LLVMOpaqueValue))

            LLVMGetMDNodeOperands(metadataNode, operandArray[0])!!

            return operandArray[0].value!!
        }
    }

    fun close() {
        LLVMDisposeModule(llvmModule)
        LLVMContextDispose(llvmContext)
    }
}


internal class MetadataGenerator(override val context: Context): ContextUtils {

    private fun metadataString(str: String): LLVMOpaqueValue {
        return LLVMMDString(str, str.length)!!
    }

    private fun metadataNode(args: List<LLVMOpaqueValue?>): LLVMOpaqueValue {
        memScoped {
            val references = alloc(array[args.size](Ref to LLVMOpaqueValue))
            args.forEachIndexed { i, llvmOpaqueValue ->  references[i].value = args[i]}
            return LLVMMDNode(references[0], args.size)!!
        }
    }

    private fun metadataFun(fn: LLVMOpaqueValue?, info: String): LLVMOpaqueValue {
        val args = listOf(fn, metadataString(info));
        val md = metadataNode(args)
        return md
    }

    private fun emitModuleMetadata(name: String, md: LLVMOpaqueValue?) {
        LLVMAddNamedMetadataOperand(context.llvmModule, name, md)
    }

    private fun protobufType(type:KotlinType): TypeLinkdata {
        val builder = TypeLinkdata.newBuilder()
        val name = type.toString()
        val proto = builder
            .setName(name)
            .build()

        return proto
    }

    private fun serializeFunSignature(declaration: IrFunction): String {
        val func = declaration.descriptor

        val hash = "0x123456 some hash"
        val name = func.name.asString()

        val kotlinType = func.getReturnType()!!
        val returnType = protobufType(kotlinType)

        val parameters = func.getValueParameters()
        val argumentTypes = parameters.map{ protobufType(it.getType()) }

        val proto = FunLinkdata.newBuilder()
            .setHash(hash)
            .setName(name)
            .setReturnType(returnType)
            .addAllArg(argumentTypes)
            .build()

        // Convert it to ProtoBuf's TextFormat representation.
        // Use TextFormat.merge(str, builder) to parse it back
        val str = proto.toString()
        return str
    }

    internal fun function(declaration: IrFunction) {
        val fn = declaration.descriptor.llvmFunction.getLlvmValue()

        val proto = serializeFunSignature(declaration)

        val md = metadataFun(fn, proto)
        emitModuleMetadata("kfun", md);
    }

    // Quick check consistency
    private fun debug_blob(blob: String) {
        var metadataList = mutableListOf<KotlinKonanMetadata>()
        KotlinKonanMetadataUtils.parseMetadata(blob, metadataList)

        val body = metadataList.first().body
        val gzipInputStream = java.util.zip.GZIPInputStream(ByteArrayInputStream(body))
        val content = JsProtoBuf.Library.parseFrom(gzipInputStream)
        gzipInputStream.close()

        println(content.toString())
        println(blob)
    }

    private fun serializeModule(moduleDescriptor: ModuleDescriptor): String {
        val description = JsModuleDescriptor(
                name = "foo",
                kind = ModuleKind.PLAIN,
                imported = listOf(),
                data = moduleDescriptor
             )

        // TODO: eliminate this dependency on JavaScript compiler
        val blobString =  KotlinJavascriptSerializationUtil.metadataAsString(description)
        if (false) {
            debug_blob(blobString)
        }
        return blobString

    }

    internal fun endModule(module: IrModuleFragment) {
        val moduleAsString = serializeModule(module.descriptor)
        val stringNode = metadataString(moduleAsString)
        emitModuleMetadata("kmetadata", stringNode)
    }
}



