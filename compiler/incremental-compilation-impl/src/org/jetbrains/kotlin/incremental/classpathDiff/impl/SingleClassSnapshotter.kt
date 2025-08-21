/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff.impl

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade.Companion.getNonPrivateMembers
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.classpathDiff.*
import org.jetbrains.kotlin.incremental.classpathDiff.impl.SingleClassSnapshotter.snapshotJavaClass
import org.jetbrains.kotlin.incremental.classpathDiff.impl.SingleClassSnapshotter.snapshotKotlinClass
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotClass
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotClassExcludingMembers
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotField
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.snapshotMethod
import org.jetbrains.kotlin.incremental.impl.ClassNodeSnapshotter.sortClassMembers
import org.jetbrains.kotlin.incremental.impl.SelectiveClassVisitor
import org.jetbrains.kotlin.incremental.impl.hashToLong
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode


/**
 * We can have different approaches to classpath snapshotting organization - caching and parallelizing different parts of work,
 * calculating (or not calculating) snapshots of inlined inaccessible classes, etc.
 *
 * On the other hand, core [snapshotKotlinClass] and [snapshotJavaClass] utils are not supposed to change very often.
 */
internal object SingleClassSnapshotter {

    /** Computes a [KotlinClassSnapshot] of the given Kotlin class. */
    fun snapshotKotlinClass(
        classFile: ClassFileWithContents,
        granularity: ClassSnapshotGranularity,
        kotlinClassInfo: KotlinClassInfo,
    ): KotlinClassSnapshot {
        val classId = kotlinClassInfo.classId
        val classAbiHash = KotlinClassInfoExternalizer.toByteArray(kotlinClassInfo).hashToLong()
        val classMemberLevelSnapshot = kotlinClassInfo.takeIf { granularity == CLASS_MEMBER_LEVEL }

        return when (kotlinClassInfo.classKind) {
            CLASS -> RegularKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                supertypes = classFile.classInfo.supertypes,
                companionObjectName = kotlinClassInfo.companionObject?.shortClassName?.identifier,
                constantsInCompanionObject = kotlinClassInfo.constantsInCompanionObject
            )
            FILE_FACADE, MULTIFILE_CLASS_PART -> PackageFacadeKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                packageMemberNames = (kotlinClassInfo.protoData as PackagePartProtoData).getNonPrivateMembers().toSet()
            )
            MULTIFILE_CLASS -> MultifileClassKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                constantNames = kotlinClassInfo.extraInfo.constantSnapshots.keys
            )
            SYNTHETIC_CLASS -> error("Unexpected class $classId with class kind ${SYNTHETIC_CLASS.name} (synthetic classes should have been removed earlier)")
            UNKNOWN -> error("Can't handle class $classId with class kind ${UNKNOWN.name}")
        }
    }

    /** Computes a [JavaClassSnapshot] of the given Java class. */
    fun snapshotJavaClass(classFile: ClassFileWithContents, granularity: ClassSnapshotGranularity): JavaClassSnapshot {
        // For incremental compilation, we only care about the ABI info of a class. There are 2 approaches:
        //   1. Collect ABI info directly
        //   2. Remove non-ABI info from the full class
        // Note that for incremental compilation to be correct, all ABI info must be collected exhaustively (now and in the future when
        // there are updates to Java/ASM), whereas it is acceptable if non-ABI info is not removed completely.
        // Therefore, we will use the second approach as it is safer and easier.

        // 1. Create a ClassNode that will contain ABI info of the class
        val classNode = ClassNode()

        // 2. Load the class's contents into the ClassNode, removing non-ABI info:
        //     - Remove private fields and methods
        //     - Remove method bodies
        //     - [Not yet implemented] Ignore fields' values except for constants
        // Note the `parsingOptions` passed to `classReader`:
        //     - Pass SKIP_CODE as we want to remove method bodies
        //     - Do not pass SKIP_DEBUG as debug info (e.g., method parameter names) may be important
        val classReader = ClassReader(classFile.contents)
        val selectiveClassVisitor = SelectiveClassVisitor(
            classNode,
            shouldVisitField = { _: JvmMemberSignature.Field, isPrivate: Boolean, _: Boolean -> !isPrivate },
            shouldVisitMethod = { _: JvmMemberSignature.Method, isPrivate: Boolean -> !isPrivate }
        )
        classReader.accept(selectiveClassVisitor, ClassReader.SKIP_CODE)

        // 3. Sort fields and methods as their order is not important
        sortClassMembers(classNode)

        // 4. Snapshot the class
        val classMemberLevelSnapshot = if (granularity == CLASS_MEMBER_LEVEL) {
            JavaClassMemberLevelSnapshot(
                classAbiExcludingMembers = JavaElementSnapshot(classNode.name, snapshotClassExcludingMembers(classNode)),
                fieldsAbi = classNode.fields.map { JavaElementSnapshot(it.name, snapshotField(it)) },
                methodsAbi = classNode.methods.map { JavaElementSnapshot(it.name, snapshotMethod(it, classNode.version)) }
            )
        } else {
            null
        }
        val classAbiHash = if (granularity == CLASS_MEMBER_LEVEL) {
            // We already have the class-member-level snapshot, so we can just hash it instead of snapshotting the class again
            JavaClassMemberLevelSnapshotExternalizer.toByteArray(classMemberLevelSnapshot!!).hashToLong()
        } else {
            snapshotClass(classNode)
        }

        return JavaClassSnapshot(
            classId = classFile.classInfo.classId,
            classAbiHash = classAbiHash,
            classMemberLevelSnapshot = classMemberLevelSnapshot,
            supertypes = classFile.classInfo.supertypes
        )
    }
}
