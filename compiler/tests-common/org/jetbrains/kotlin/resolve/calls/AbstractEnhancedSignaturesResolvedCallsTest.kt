/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind

abstract class AbstractEnhancedSignaturesResolvedCallsTest : AbstractResolvedCallsTest() {
    // requires full JDK with various Java API: java.util.Optional, java.util.Map
    override fun createEnvironment(): KotlinCoreEnvironment = createEnvironmentWithJdk(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)

    override fun renderOutput(originalText: String, text: String, resolvedCallsAt: List<Pair<Int, ResolvedCall<*>?>>): String {
        val lines = text.lines()
        val lineOffsets = run {
            var offset = 0
            lines.map { offset.apply { offset += it.length + 1 /* new-line delimiter */ } }
        }
        fun lineIndexAt(caret: Int): Int =
                lineOffsets.binarySearch(caret).let { result ->
                    if (result < 0) result.inv() - 1 else result }


        val callsByLine = resolvedCallsAt.groupBy ({ (caret) -> lineIndexAt(caret) }, { (_, resolvedCall) -> resolvedCall })

        return buildString {
            lines.forEachIndexed { lineIndex, line ->
                appendln(line)
                callsByLine[lineIndex]?.let { calls ->
                    val indent = line.takeWhile(Char::isWhitespace) + "    "
                    calls.forEach { resolvedCall ->
                        appendln("$indent// ${resolvedCall?.status}")
                        appendln("$indent// ORIGINAL:    ${resolvedCall?.run { resultingDescriptor!!.original.getText() }}")
                        appendln("$indent// SUBSTITUTED: ${resolvedCall?.run { resultingDescriptor!!.getText() }}")
                    }
                }
            }
        }
    }

}