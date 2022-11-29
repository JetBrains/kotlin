/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

/** Computes a [JavaClassSnapshot] of a Java class. */
object JavaClassSnapshotter {

    fun snapshot(classFile: ClassFileWithContents, granularity: ClassSnapshotGranularity): JavaClassSnapshot {
        // We will extract ABI information from the given class and store it into the `abiClass` variable.
        // It is acceptable to collect more info than required, but it is incorrect to collect less info than required.
        // There are 2 approaches:
        //   1. Collect ABI info directly. The collected info must be exhaustive (now and in the future when there are updates to Java/ASM).
        //   2. Collect all info and remove non-ABI info. The removed info should be exhaustive, but even if it's not, it is still
        //      acceptable.
        // In the following, we will use the second approach as it is safer.
        val abiClass = ClassNode()

        // First, collect all info.
        // Note the parsing options passed to ClassReader:
        //   - SKIP_CODE is set as method bodies will not be part of the ABI of the class.
        //   - SKIP_DEBUG seems possible but is *not* set just to be safe, so that we don't skip any info that might be important.
        //   - SKIP_FRAMES or EXPAND_FRAMES is not needed (and not relevant when SKIP_CODE is set).
        val classReader = ClassReader(classFile.contents)
        classReader.accept(abiClass, ClassReader.SKIP_CODE)

        // Then, remove non-ABI info, which includes:
        //   - Method bodies: Should have already been removed (see SKIP_CODE above)
        //   - Private fields and methods
        fun Int.isPrivate() = (this and Opcodes.ACC_PRIVATE) != 0
        abiClass.fields.removeIf { it.access.isPrivate() }
        abiClass.methods.removeIf { it.access.isPrivate() }

        // Normalize the class: Sort fields and methods as their order is not important (we still use List instead of Set as we want the
        // serialized snapshot to be deterministic).
        abiClass.fields.sortWith(compareBy({ it.name }, { it.desc }))
        abiClass.methods.sortWith(compareBy({ it.name }, { it.desc }))

        // Snapshot the class
        val classAbiHash = snapshotClass(abiClass)
        val classMemberLevelSnapshot = if (granularity == CLASS_MEMBER_LEVEL) {
            val fieldsAbi = abiClass.fields.map { JavaElementSnapshot(it.name, snapshotField(it)) }
            val methodsAbi = abiClass.methods.map { JavaElementSnapshot(it.name, snapshotMethod(it)) }
            val classAbiExcludingMembers = abiClass.let {
                it.fields.clear()
                it.methods.clear()
                JavaElementSnapshot(it.name, snapshotClass(it))
            }
            JavaClassMemberLevelSnapshot(classAbiExcludingMembers, fieldsAbi, methodsAbi)
        } else null

        return JavaClassSnapshot(
            classId = classFile.classInfo.classId,
            classAbiHash = classAbiHash,
            classMemberLevelSnapshot = classMemberLevelSnapshot,
            supertypes = classFile.classInfo.supertypes
        )
    }

    private fun snapshotClass(classNode: ClassNode): Long {
        val classWriter = ClassWriter(0)
        classNode.accept(classWriter)
        return classWriter.toByteArray().hashToLong()
    }

    private fun snapshotField(fieldNode: FieldNode): Long {
        val classNode = emptyClass()
        classNode.fields.add(fieldNode)
        return snapshotClass(classNode)
    }

    private fun snapshotMethod(methodNode: MethodNode): Long {
        val classNode = emptyClass()
        classNode.methods.add(methodNode)
        return snapshotClass(classNode)
    }

    private fun emptyClass() = ClassNode().also {
        // We need to provide some minimal info to the class:
        //   - Name is required.
        //   - Class version is required if method bodies are considered, but we have removed method bodies in this class, so it's optional.
        //   - Other info is optional.
        it.name = "EmptyClass"
    }
}
