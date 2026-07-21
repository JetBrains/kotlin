/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId

/**
 * Materializes a binary `.class`/`.sig` into a [BinaryJavaClass], with caching and inner-class dispatch.
 *
 * [resolveCrossReference] must cover the whole classpath, not only the caller's search scope.
 * Uses a fresh [ClassifierResolutionContext] per top-level class (the context is mutable).
 */
fun readBinaryJavaClass(
    classId: ClassId,
    topLevelVirtualFile: VirtualFile,
    classFileContent: ByteArray?,
    outerClassFromRequest: JavaClass?,
    binaryCache: MutableMap<ClassId, JavaClass?>,
    signatureParser: BinaryClassSignatureParser,
    findOuterClass: (ClassId) -> JavaClass?,
    resolveCrossReference: (ClassId) -> JavaClass?,
): JavaClass? = binaryCache.getOrPut(classId) {
    val outerClassId = classId.outerClassId
    if (outerClassId != null) {
        val outerClass = outerClassFromRequest ?: findOuterClass(outerClassId)
        return@getOrPut if (outerClass is BinaryJavaClass) {
            outerClass.findInnerClass(classId.shortClassName, classFileContent)
        } else {
            outerClass?.findInnerClass(classId.shortClassName)
        }
    }

    val classContent = classFileContent ?: topLevelVirtualFile.contentsToByteArray()
    // Class files with '$' in the name may still be nested and must not be treated as top-level.
    if (topLevelVirtualFile.nameWithoutExtension.contains("$") && isNotTopLevelClass(classContent)) {
        return@getOrPut null
    }

    val resolver = ClassifierResolutionContext(resolveCrossReference)

    BinaryJavaClass(
        topLevelVirtualFile,
        classId.asSingleFqName(),
        resolver,
        signatureParser,
        outerClass = null,
        classContent = classContent,
    )
}
