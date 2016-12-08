package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.konan.Context
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

    reader.use {
        val metadataNode = reader.namedMetadataNode("kmetadata", 0);
        val stringNode = reader.metadataOperand(metadataNode, 0)
        return reader.string(stringNode)
    }
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

    fun namedMetadataNode(name: String, index: Int): LLVMValueRef {
        memScoped {
            val nodeCount = LLVMGetNamedMetadataNumOperands(llvmModule, "kmetadata")
            val nodeArray = allocArray<LLVMValueRefVar>(nodeCount)

            LLVMGetNamedMetadataOperands(llvmModule, "kmetadata", nodeArray[0].ptr)

            return nodeArray[0].value!!
        }
    }

    fun metadataOperand(metadataNode: LLVMValueRef, index: Int): LLVMValueRef {
        memScoped {
            val operandCount = LLVMGetMDNodeNumOperands(metadataNode)
            val operandArray = allocArray<LLVMValueRefVar>(operandCount)

            LLVMGetMDNodeOperands(metadataNode, operandArray[0].ptr)

            return operandArray[0].value!!
        }
    }

    override fun close() {
        LLVMDisposeModule(llvmModule)
        LLVMContextDispose(llvmContext)
    }
}


internal class MetadataGenerator(override val context: Context): ContextUtils {

    private fun metadataString(str: String): LLVMValueRef {
        return LLVMMDString(str, str.length)!!
    }

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

    private fun emitModuleMetadata(name: String, md: LLVMValueRef?) {
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
        val fn = declaration.descriptor.llvmFunction!!

        val proto = serializeFunSignature(declaration)

        val md = metadataFun(fn, proto)
        emitModuleMetadata("kfun", md);
    }

    // Quick check consistency
    private fun debug_blob(blob: String) {
        val metadataList = mutableListOf<KotlinKonanMetadata>()
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
        val blobString =  KotlinJavascriptSerializationUtil.metadataAsString(
                context.bindingContext, description)
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

