package org.jetbrains.kotlin.backend.native.llvm


import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.backend.BinaryMetadata
import org.jetbrains.kotlin.backend.BinaryMetadata.FunMetadata

internal class MetadataGenerator(override val context: Context): ContextUtils {

    private fun metadataString(str: String): LLVMOpaqueValue {
        return LLVMMDString(str, str.length)!!
    }

    private fun metadataNode(args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue {
        memScoped {
            val rargs = alloc(array[args.size](Ref to LLVMOpaqueValue))
            args.forEachIndexed { i, llvmOpaqueValue ->  rargs[i].value = args[i]}
            return LLVMMDNode(rargs[0], args.size)!!
        }
    }

    private fun metadataFun(fn: LLVMOpaqueValue?, info: String): LLVMOpaqueValue {
        val args: MutableList<LLVMOpaqueValue?> = mutableListOf(fn, metadataString(info));
        val md = metadataNode(args)
        return md
    }

    private fun emitModuleMetadata(name: String, md: LLVMOpaqueValue?) {
        LLVMAddNamedMetadataOperand(context.llvmModule, name, md)
    }

    private fun serializeFunSignature(declaration:IrFunction): String {
        val builder = FunMetadata.newBuilder()

        val hash = "0x123456 some hash"
        val name = declaration.descriptor.name.asString()

        val proto = builder
            .setHash(hash)
            .setName(name)
            .build()

        // Convert it to ProtoBuf's TextFormat representation.
        // Use TextFormat.merge(str, builder) to parse it back
        val str = proto.toString()

        return  str
    }

    internal fun function(declaration: IrFunction) {
        val fn = declaration.descriptor.llvmFunction.getLlvmValue()

        val proto = serializeFunSignature(declaration)

        val md = metadataFun(fn, proto)
        emitModuleMetadata("kfun", md);
    }
}


