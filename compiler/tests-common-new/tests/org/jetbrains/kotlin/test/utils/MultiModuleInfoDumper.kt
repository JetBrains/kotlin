/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.model.TestModule

abstract class MultiModuleInfoDumper {
    abstract fun builderForModule(module: TestModule): StringBuilder
    abstract fun generateResultingDump(): String
    abstract fun isEmpty(): Boolean
}

// TODO: consider about tests with multiple testdata files
class MultiModuleInfoDumperImpl(private val moduleHeaderTemplate: String = "Module: %s") : MultiModuleInfoDumper() {
    private val builderByModule = LinkedHashMap<TestModule, StringBuilder>()

    override fun builderForModule(module: TestModule): StringBuilder {
        return builderByModule.getOrPut(module, ::StringBuilder)
    }

    override fun generateResultingDump(): String {
        builderByModule.values.singleOrNull()?.let { return it.toString() }
        return buildString {
            for ((module, builder) in builderByModule) {
                appendLine(String.format(moduleHeaderTemplate, module.name))
                append(builder)
            }
        }
    }

    override fun isEmpty(): Boolean {
        return builderByModule.isEmpty()
    }
}
