/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.model.TestModule

// TODO: consider about tests with multiple testdata files
class MultiModuleInfoDumper(private val moduleHeaderTemplate: String? = "Module: %s") {
    private val builderByModule = LinkedHashMap<String, StringBuilder>()

    fun builderForModule(module: TestModule): StringBuilder = builderForModule(module.name)

    fun builderForModule(moduleName: String): StringBuilder {
        return builderByModule.getOrPut(moduleName, ::StringBuilder)
    }

    fun generateResultingDump(): String {
        builderByModule.values.singleOrNull()?.let {
            it.addNewLineIfNeeded()
            return it.toString()
        }
        return buildString {
            for ((moduleName, builder) in builderByModule) {
                moduleHeaderTemplate?.let { appendLine(String.format(it, moduleName)) }
                append(builder)
            }
            addNewLineIfNeeded()
        }
    }

    fun isEmpty(): Boolean {
        return builderByModule.isEmpty()
    }

    private fun StringBuilder.addNewLineIfNeeded() {
        if (this.isEmpty()) return
        if (last() != '\n') {
            appendLine()
        }
    }
}
