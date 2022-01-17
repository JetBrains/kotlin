/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode

/** Computes a [JavaClassSnapshot] of a Java class. */
object JavaClassSnapshotter {

    fun snapshot(
        classFile: ClassFileWithContents,
        granularity: ClassSnapshotGranularity,
        includeDebugInfoInSnapshot: Boolean
    ): JavaClassSnapshot {
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
        //   - SKIP_CODE and SKIP_FRAMES are set as method bodies will not be part of the ABI of the class.
        //   - SKIP_DEBUG is not set as it would skip method parameters, which may be used by annotation processors like Room.
        //   - EXPAND_FRAMES is not needed (and not relevant when SKIP_CODE is set).
        val classReader = ClassReader(classFile.contents)
        classReader.accept(abiClass, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)

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
        val fieldsAbi = abiClass.fields.map { snapshotJavaElement(it, it.name, includeDebugInfoInSnapshot) }
        val methodsAbi = abiClass.methods.map { snapshotJavaElement(it, it.name, includeDebugInfoInSnapshot) }

        abiClass.fields.clear()
        abiClass.methods.clear()
        val classAbiExcludingMembers = abiClass.let { snapshotJavaElement(it, it.name, includeDebugInfoInSnapshot) }

        val detailedSnapshot = JavaClassMemberLevelSnapshot(classAbiExcludingMembers, fieldsAbi, methodsAbi)
        return JavaClassSnapshot(
            classId = classFile.classInfo.classId,
            classAbiHash = JavaClassMemberLevelSnapshotExternalizer.toByteArray(detailedSnapshot).md5(),
            classMemberLevelSnapshot = detailedSnapshot.takeIf { granularity == CLASS_MEMBER_LEVEL },
            supertypes = classFile.classInfo.supertypes
        )
    }

    private val gson by lazy {
        // Use serializeSpecialFloatingPointValues() to avoid this error
        //    "java.lang.IllegalArgumentException: NaN is not a valid double value as per JSON specification. To override this behavior, use
        //    GsonBuilder.serializeSpecialFloatingPointValues() method."
        // on jars such as ~/.gradle/kotlin-build-dependencies/repo/kotlin.build/ideaIC/203.8084.24/artifacts/lib/rhino-1.7.12.jar.
        GsonBuilder().serializeSpecialFloatingPointValues().create()
    }

    // Same as above but with `setPrettyPrinting()`
    private val gsonForDebug by lazy {
        GsonBuilder().serializeSpecialFloatingPointValues()
            .setPrettyPrinting()
            .create()
    }

    private fun snapshotJavaElement(
        javaElement: Any,
        javaElementName: String,
        includeDebugInfoInSnapshot: Boolean
    ): JavaElementSnapshot {
        return if (includeDebugInfoInSnapshot) {
            val abiValue = gsonForDebug.toJson(javaElement)
            val abiHash = abiValue.toByteArray().md5()
            JavaElementSnapshotForTests(javaElementName, abiHash, abiValue)
        } else {
            val abiValue = gson.toJson(javaElement)
            val abiHash = abiValue.toByteArray().md5()
            JavaElementSnapshot(javaElementName, abiHash)
        }
    }
}
