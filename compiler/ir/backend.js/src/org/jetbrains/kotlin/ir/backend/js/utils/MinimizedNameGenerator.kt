/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

class MinimizedNameGenerator {
    private var index = 0
    private val functionSignatureToName = hashMapOf<String, String>()
    private val reservedNames = hashSetOf<String>()
    private val keptNames = hashSetOf<String>()

    fun generateNextName(): String {
        var candidate = index++.toJsIdentifier()
        while (candidate in reservedNames) {
            candidate = index++.toJsIdentifier()
        }
        return candidate
    }

    fun nameBySignature(signature: String): String {
        if (signature in keptNames) return signature
        return functionSignatureToName.getOrPut(signature) {
            generateNextName()
        }
    }

    fun keepName(signature: String): Boolean {
        return keptNames.add(signature)
    }

    fun reserveName(signature: String) {
        reservedNames.add(signature)
    }

    fun clear() {
        index = 0
        functionSignatureToName.clear()
        reservedNames.clear()
    }
}
