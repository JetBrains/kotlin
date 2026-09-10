/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import com.google.common.hash.Hashing
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode


private val fingerprint = Hashing.farmHashFingerprint64()

fun ByteArray.hashToLong(): Long {
    // Note: The returned type `Long` is 64-bit, but we currently don't have a good 64-bit hash function.
    // The method below uses `md5` which is 128-bit and converts it to `Long`.
    return fingerprint.hashBytes(this).asLong()
}

fun classNode(classFile: ByteArray): ClassNode {
    return classNode(ClassReader(classFile))
}

fun classNode(classReader: ClassReader): ClassNode {
    val classNode = ClassNode()
    classReader.accept(classNode, 0)
    return classNode
}
