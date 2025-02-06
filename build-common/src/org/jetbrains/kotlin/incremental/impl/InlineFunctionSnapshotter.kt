/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor

private class InlineFunctionsSpecialSupportClassVisitor(
    val classNode: ClassNode,
    val context: ClassInfoGeneratorContextWithLocalClassSnapshotting
) : ClassVisitor(Opcodes.ASM9, classNode) {
    // TODO check whether we need a textifier here - e.g. lambdas without debug info change - replace 1 with 3 etc

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String?>?
    ): MethodVisitor? {
        val textifier = Textifier()
        val methodVisitor = object : MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            val methodSignature =
                name?.let { descriptor?.let { JvmMemberSignature.Method(name, descriptor) } }?.let { signature ->
                    ClassInfoGeneratorContextWithLocalClassSnapshotting.MethodWithOwner(classNode.name, signature)
                }

            override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
                if (opcode == Opcodes.GETSTATIC && name == "INSTANCE") {
                    ///println("getstaticing $owner $name $descriptor")
                    context.addFqNameUsage(methodSignature!!, FqName(owner!!))
                    //TODO why is nullability so bad here, fix !!
                }
                super.visitFieldInsn(opcode, owner, name, descriptor)
            }
        }
        return TraceMethodVisitor(methodVisitor, textifier)
    }
}

object InlineFunctionSnapshotter {
    //TODO relevant to other TODOs - are we only using this for accessible classes? :)
    fun getAccessibleClassVisitor(
        classNode: ClassNode,
        context: ClassInfoGeneratorContextWithLocalClassSnapshotting
    ): ClassVisitor {
        return InlineFunctionsSpecialSupportClassVisitor(classNode, context)
    }

    fun getFullClassSnapshot(classContents: ByteArray): Long {
        val classNode = ClassNode()
        val textifier = Textifier()
        //val classVisitor = TraceClassVisitor(classNode, textifier, null)
        //ClassReader(classContents).accept(classVisitor, 0)

       // val symbolTable = SymbolTable()

        //aha, okay
        //TODO: so basically, normal TraceClassVisitor skips the constant pool, and sometimes it's the pitfall
        // let's go in the other direction: use full file hash first, add test to account for insignificant changes, then think
        //TODO surely i need to think (btw ClassReader exposes constants well enough)

        // it is a possible optimization to create one instance per transform and reuse it
//        val digester = MessageDigest.getInstance("MD5")
//        for (item in textifier.getText()) {
//            if (item is String) {
//                digester.update(item.toByteArray())
//            }
//        }
//        return digestedByteArrayToLong(digester.digest())
        return classContents.hashToLong()
    }
}
