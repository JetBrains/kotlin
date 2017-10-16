/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen

interface NativeScope {
    val mappingBridgeGenerator: MappingBridgeGenerator
}

class NativeCodeBuilder(val scope: NativeScope) {
    val lines = mutableListOf<String>()

    fun out(line: String): Unit {
        lines.add(line)
    }
}

inline fun buildNativeCodeLines(scope: NativeScope, block: NativeCodeBuilder.() -> Unit): List<String> {
    val builder = NativeCodeBuilder(scope)
    builder.block()
    return builder.lines
}

class KotlinCodeBuilder(val scope: KotlinScope) {
    private val lines = mutableListOf<String>()

    private val freeStack = mutableListOf<String>()
    private val nesting get() = freeStack.size

    fun out(line: String) {
        lines.add("    ".repeat(nesting) + line)
    }

    private var memScoped = false
    fun pushMemScoped() {
        if (!memScoped) {
            memScoped = true
            pushBlock("memScoped {")
        }
    }

    fun pushBlock(line: String, free: String = "") {
        out(line)
        freeStack.add(free)
    }

    private fun popBlocks() {
        while (freeStack.isNotEmpty()) {
            val free = freeStack.last()
            freeStack.removeAt(freeStack.lastIndex)
            out("} $free".trim())
        }
    }

    fun build(): List<String> {
        this.popBlocks()
        val result = this.lines.toList()
        this.lines.clear()
        return result
    }
}

inline fun buildKotlinCodeLines(scope: KotlinScope, block: KotlinCodeBuilder.() -> Unit): List<String> {
    val builder = KotlinCodeBuilder(scope)
    builder.block()
    return builder.build()
}

interface StubGenerationContext {
    val nativeBridges: NativeBridges
    fun addTopLevelDeclaration(lines: List<String>)
}

interface KotlinStub {
    fun generate(context: StubGenerationContext): Sequence<String>
}