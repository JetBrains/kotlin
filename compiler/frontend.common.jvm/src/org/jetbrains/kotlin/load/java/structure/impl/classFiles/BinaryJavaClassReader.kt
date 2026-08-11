/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId

/**
 * Loads the [BinaryJavaClass] for [classId] from a `.class`/`.sig` file, caching it in [binaryCache];
 * a nested [classId] is loaded through its outer class.
 *
 * [resolveCrossReference] must cover the whole classpath, to resolve the references recorded in the bytecode:
 * they are bound to the classpath the class was compiled against, which is wider than the search scope the
 * class is requested from.
 * Uses a fresh [ClassifierResolutionContext] per top-level class (the context is mutable).
 */
fun readBinaryJavaClass(
    classId: ClassId,
    topLevelClassFile: BinaryClassFileHandle,
    classFileContent: ByteArray?,
    outerClassFromRequest: JavaClass?,
    binaryCache: BinaryJavaClasses,
    signatureParser: BinaryClassSignatureParser,
    findOuterClass: (ClassId) -> JavaClass?,
    resolveCrossReference: (ClassId) -> JavaClass?,
): JavaClass? = binaryCache.getOrPut(topLevelClassFile, classId) {
    val outerClassId = classId.outerClassId
    if (outerClassId != null) {
        val outerClass = outerClassFromRequest ?: findOuterClass(outerClassId)
        return@getOrPut if (outerClass is BinaryJavaClass) {
            outerClass.findInnerClass(classId.shortClassName, classFileContent)
        } else {
            outerClass?.findInnerClass(classId.shortClassName)
        }
    }

    val classContent = classFileContent ?: topLevelClassFile.readBytes()
    // A '$' in the file name may come from a nested class instead of a top-level class named with '$':
    // such a class has no top-level id and is only reachable through its outer class.
    if (topLevelClassFile.nameWithoutExtension.contains("$") && isNotTopLevelClass(classContent)) {
        return@getOrPut null
    }

    val resolver = ClassifierResolutionContext(resolveCrossReference)

    BinaryJavaClass(
        topLevelClassFile.virtualFile,
        classId.asSingleFqName(),
        resolver,
        signatureParser,
        outerClass = null,
        classContent = classContent,
    )
}

/**
 * The classes read from binary class files, indexed by the class file they were read from *and* by the
 * [ClassId] inside it: a class file declares its top-level class together with every class nested in it,
 * and the same [ClassId] may be declared by several classpath roots.
 */
class BinaryJavaClasses {
    private val classesByFile: MutableMap<BinaryClassFileHandle, MutableMap<ClassId, JavaClass?>> = HashMap()

    // A `null` is not remembered, so a class which was not found is looked for again on the next request.
    internal fun getOrPut(classFile: BinaryClassFileHandle, classId: ClassId, read: () -> JavaClass?): JavaClass? =
        classesByFile.getOrPut(classFile) { HashMap() }.getOrPut(classId, read)
}

fun VirtualFile.asBinaryClassFileHandle(): BinaryClassFileHandle = VirtualFileBinaryClassFileHandle(this)

private class VirtualFileBinaryClassFileHandle(val virtualFile: VirtualFile) : BinaryClassFileHandle {
    /** The content version of [virtualFile] at the time of the creation of this handle. */
    private val modificationStamp: Long = virtualFile.modificationStamp

    override val nameWithoutExtension: String
        get() = virtualFile.nameWithoutExtension

    override fun readBytes(): ByteArray = virtualFile.contentsToByteArray()

    override fun equals(other: Any?): Boolean =
        this === other ||
                other is VirtualFileBinaryClassFileHandle &&
                virtualFile == other.virtualFile &&
                modificationStamp == other.modificationStamp

    override fun hashCode(): Int = virtualFile.hashCode()

    override fun toString(): String = virtualFile.toString()
}

val BinaryClassFileHandle.virtualFile: VirtualFile
    get() = (this as VirtualFileBinaryClassFileHandle).virtualFile
