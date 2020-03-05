/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util

import org.jetbrains.kotlin.test.KotlinBaseTest.TestFile

enum class SteppingInstructionKind(val directiveName: String) {
    StepInto("STEP_INTO"),
    StepOut("STEP_OUT"),
    StepOver("STEP_OVER"),
    ForceStepOver("STEP_OVER_FORCE"),
    SmartStepInto("SMART_STEP_INTO"),
    SmartStepIntoByIndex("SMART_STEP_INTO_BY_INDEX"),
    Resume("RESUME")
}

class SteppingInstruction(val kind: SteppingInstructionKind, val arg: Int) {
    companion object {
        fun parse(file: TestFile): List<SteppingInstruction> {
            return parse(file, Companion::parseLine)
        }

        fun parseSingle(file: TestFile, kind: SteppingInstructionKind): SteppingInstruction? {
            val instructions = parse(file) { line -> parseKind(line, kind) }
            if (instructions.size > 1) {
                error("Several instructions found for kind $kind")
            }

            return instructions.singleOrNull()
        }

        private fun parse(file: TestFile, processor: (String) -> SteppingInstruction?): List<SteppingInstruction> {
            return file.content.lineSequence()
                .map { it.trimStart() }
                .filter { it.startsWith("// ") }
                .mapNotNullTo(mutableListOf(), processor)
        }

        private fun parseLine(line: String): SteppingInstruction? {
            for (kind in SteppingInstructionKind.values()) {
                parseKind(line, kind)?.let { return it }
            }

            return null
        }

        private fun parseKind(line: String, kind: SteppingInstructionKind): SteppingInstruction? {
            val prefix = "// " + kind.directiveName + ": "
            if (line.startsWith(prefix)) {
                val rawValue = line.drop(prefix.length).trim()
                val n = rawValue.toIntOrNull() ?: error("Int expected, got $rawValue")
                return SteppingInstruction(kind, n)
            }

            return null
        }
    }
}