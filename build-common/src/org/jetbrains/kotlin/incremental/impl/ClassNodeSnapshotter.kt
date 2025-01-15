/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

/** Computes the snapshot of a Java class represented by a [ClassNode]. */
object ClassNodeSnapshotter {

    fun snapshotClass(classNode: ClassNode): Long {
        val classWriter = ClassWriter(0)
        classNode.accept(classWriter)
        return classWriter.toByteArray().hashToLong()
    }

    fun snapshotClassExcludingMembers(classNode: ClassNode, alsoExcludeKotlinMetaData: Boolean = false): Long {
        val originalFields = classNode.fields
        val originalMethods = classNode.methods
        val originalVisibleAnnotations = classNode.visibleAnnotations
        classNode.fields = emptyList()
        classNode.methods = emptyList()
        if (alsoExcludeKotlinMetaData) {
            classNode.visibleAnnotations = originalVisibleAnnotations?.filterNot { it.desc == "Lkotlin/Metadata;" }
        }
        return snapshotClass(classNode).also {
            classNode.fields = originalFields
            classNode.methods = originalMethods
            classNode.visibleAnnotations = originalVisibleAnnotations
        }
    }

    fun snapshotField(fieldNode: FieldNode): Long {
        val classNode = emptyClass()
        classNode.fields.add(fieldNode)
        return snapshotClass(classNode)
    }

    fun snapshotMethod(methodNode: MethodNode, classVersion: Int): Long {
        val classNode = emptyClass()
        classNode.version = classVersion // Class version is required when working with methods (without it, ASM may fail -- see KT-38857)
        classNode.methods.add(methodNode)
        return snapshotClass(classNode)
    }

    /**
     * Sorts fields and methods in the given class.
     *
     * This is useful when we want to ensure a change in the order of the fields and methods doesn't impact the snapshot (i.e., if their
     * order has changed in the `.class` file, it shouldn't require recompilation of the other source files).
     */
    fun sortClassMembers(classNode: ClassNode) {
        classNode.fields.sortWith(compareBy({ it.name }, { it.desc }))
        classNode.methods.sortWith(compareBy({ it.name }, { it.desc }))
    }

    private fun emptyClass() = ClassNode().also {
        // A name is required
        it.name = "SomeClass"
    }
}
