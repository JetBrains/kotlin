package org.jetbrains.kotlin.backend.konan.llvm


import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.backend.konan.llvm.BinaryLinkdata.*
import org.jetbrains.kotlin.types.KotlinType

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
}


