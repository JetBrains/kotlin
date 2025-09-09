/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.StubElement
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.psi.stubs.impl.STUB_TO_STRING_PREFIX

@TestOnly
fun StubElement<out PsiElement>.serializeToString(): String {
    return serializeStubToString(this)
}

private fun serializeStubToString(stubElement: StubElement<*>): String {
    val treeStr = DebugUtil.stubTreeToString(stubElement)

    // Nodes are stored in form "NodeType:Node" and have too many repeating information for Kotlin stubs
    // Remove all repeating information (See KotlinStubBaseImpl.toString())
    return treeStr.lines().joinToString(separator = "\n") {
        if (it.contains(STUB_TO_STRING_PREFIX)) {
            it.takeWhile(Char::isWhitespace) + it.substringAfter(STUB_TO_STRING_PREFIX)
        } else {
            it
        }
    }.replace(", [", "[")
}

