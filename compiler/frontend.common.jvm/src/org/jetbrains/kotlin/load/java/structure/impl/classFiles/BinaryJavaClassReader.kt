/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId

/**
 * Shared ASM-driven materialization of a binary `.class`/`.sig` Java class into a [BinaryJavaClass],
 * used by the index-backed binary Java readers (the CLI file manager and the deserializer seam) so
 * the two cannot drift. The caller resolves the outermost class'es virtual file (applying whatever
 * scope semantics it needs); this function only owns the caching, inner-class dispatch and the
 * `ClassifierResolutionContext` wiring that both readers share verbatim.
 *
 * @param topLevelVirtualFile virtual file of [classId]'s outermost class, already resolved (and
 *   scope-filtered) by the caller.
 * @param classFileContent raw bytes of the requested class if the caller already has them, else `null`.
 * @param outerClassFromRequest already-materialized enclosing class if the caller has it, else `null`.
 * @param binaryCache per-reader memo of resolved classes, keyed by [ClassId] and shared across scope modes.
 * @param signatureParser parser used to decode generic signatures from the bytecode.
 * @param findOuterClass recursion used to resolve the enclosing class of a nested [classId].
 * @param resolveCrossReference resolves cross-references (supertypes, parameter types, …) read from the
 *   bytecode signature; must resolve across the whole classpath, not only the caller's session scope.
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

    // Top-level class.
    val classContent = classFileContent ?: topLevelVirtualFile.contentsToByteArray()
    // Defensive: a class file whose name contains '$' but is actually nested must not be returned as
    // a top-level class.
    if (topLevelVirtualFile.nameWithoutExtension.contains("$") && isNotTopLevelClass(classContent)) {
        return@getOrPut null
    }

    // A fresh `ClassifierResolutionContext` is created per top-level call because the context is
    // mutable: it accumulates type parameters and inner-class info from every `BinaryJavaClass` it
    // materialises. Sharing a single instance across calls bleeds the type parameters of one class
    // into the resolution of an unrelated one.
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
