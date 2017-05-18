/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.library

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.util.File
import java.io.Closeable
import java.util.Properties

// We still keep this format around for now. But, most probably, it will
// go away completely soon.

class KtBcMetadataReader(file: File) : Closeable, MetadataReader {

override fun loadSerializedModule(currentAbiVersion: Int): NamedModuleData  {
    val (nodeCount, kmetadataNodeArg) = namedMetadataNode("kmetadata")

    if (nodeCount != 1) {
        throw Error("Unknown metadata format. The 'kmetadata' node has ${nodeCount} arguments. Don't know what to do.")
    }

    val operands = metadataNodeOperands(kmetadataNodeArg)

    val abiNode = operands[0]
    val nameNode = operands[1]
    val dataNode = operands[2]

    val abiVersion = string(abiNode).toInt()
    if (abiVersion != currentAbiVersion) {
        throw Error("Expected ABI version ${currentAbiVersion}, but the binary is ${abiVersion}")
    }
    val moduleName = string(nameNode)
    val tableOfContentsAsString = string(dataNode)
    return NamedModuleData(moduleName, tableOfContentsAsString)
}

override fun loadSerializedPackageFragment(fqName: String): String {
    val (nodeCount, kpackageNodeArg) = namedMetadataNode("kpackage:$fqName")

    if (nodeCount != 1) {
        throw Error("The 'kpackage:$fqName' node has ${nodeCount} arguments.")
    }

    val operands = metadataNodeOperands(kpackageNodeArg)
    val dataNode = operands[0]
    val base64 =  string(dataNode)
    return base64
}

    lateinit var llvmModule: LLVMModuleRef
    lateinit var llvmContext: LLVMContextRef

    init {
        memScoped {
            val bufRef = alloc<LLVMMemoryBufferRefVar>()
            val errorRef = allocPointerTo<ByteVar>()
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
            val len = alloc<IntVar>()
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
                result.add(nodeArray[index]!!)
            }
        }
        return result.toTypedArray<LLVMValueRef>()
    }

    fun namedMetadataNode(name: String): Pair<Int, LLVMValueRef> {
        memScoped {
            val nodeCount = LLVMGetNamedMetadataNumOperands(llvmModule, name)
            val nodeArray = allocArray<LLVMValueRefVar>(nodeCount)

            LLVMGetNamedMetadataOperands(llvmModule, name, nodeArray)

            return Pair(nodeCount, nodeArray[0]!!)
        }
    }

    fun metadataNodeOperands(metadataNode: LLVMValueRef): Array<LLVMValueRef> {
        memScoped {
            val operandCount = LLVMGetMDNodeNumOperands(metadataNode)
            val operandArray = allocArray<LLVMValueRefVar>(operandCount)

            LLVMGetMDNodeOperands(metadataNode, operandArray)

            return Array(operandCount, {index -> operandArray[index]!!})
        }
    }

    override fun close() {
        LLVMDisposeModule(llvmModule)
        LLVMContextDispose(llvmContext)
    }
}

internal class KtBcMetadataGenerator(val llvmModule: LLVMModuleRef) {

    private fun metadataNode(args: List<LLVMValueRef?>): LLVMValueRef {
        return LLVMMDNode(args.toCValues(), args.size)!!
    }

    private fun metadataFun(fn: LLVMValueRef, info: String): LLVMValueRef {
        val args = listOf(fn, metadataString(info));
        val md = metadataNode(args)
        return md
    }

    private fun emitModuleMetadata(name: String, md: LLVMValueRef?) {
        LLVMAddNamedMetadataOperand(llvmModule, name, md)
    }

    private fun metadataString(str: String): LLVMValueRef {
        return LLVMMDString(str, str.length)!!
    }

    fun addLinkData(linkData: LinkData) {
        if (linkData == null) return

        val abiNode = metadataString("${linkData.abiVersion}")
        val moduleNameNode = metadataString(linkData.moduleName)
        val module = linkData.module
        val fragments = linkData.fragments
        val fragmentNames = linkData.fragmentNames
        val dataNode = metadataString(module)

        val kmetadataArg  = metadataNode(listOf(abiNode, moduleNameNode, dataNode))
        emitModuleMetadata("kmetadata", kmetadataArg)

        fragments.forEachIndexed { index, it ->
            val name = fragmentNames[index]
            val dataNode = metadataString(it)
            val kpackageArg = metadataNode(listOf(dataNode))
            emitModuleMetadata("kpackage:$name", kpackageArg)
        }
    }
}

