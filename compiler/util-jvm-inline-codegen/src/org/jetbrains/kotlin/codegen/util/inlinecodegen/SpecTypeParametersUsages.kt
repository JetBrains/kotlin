/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

data class SpecTypeParametersUsages(
    val parameterGenericIndices: Map<Int, Usage>,
    val returnType: Usage?,
) {
    val isNotEmpty: Boolean get() = parameterGenericIndices.isNotEmpty() || returnType != null

    data class Usage(val genericIndex: Int, val nullable: Boolean) {
        fun adjustType(typeParameters: Map<Int, LightIrType>): LightIrType? {
            val type = typeParameters[genericIndex] ?: return null
            if (nullable) return type.markNullable()
            return type
        }

        fun encode() = "$genericIndex${if (nullable) "N" else ""}"

        companion object {
            fun decode(str: String): Usage {
                val nullable = str.endsWith("N")
                val index = str.removeSuffix("N").toInt()
                return Usage(index, nullable)
            }
        }
    }

    fun encode(): String {
        return buildString {
            parameterGenericIndices.forEach { (k, v) -> appendLine("$k=${v.encode()}") }
            returnType?.let { appendLine("ret=${it.encode()}") }
        }
    }

    companion object {
        fun decode(str: String): SpecTypeParametersUsages {
            val parameterGenericIndices = HashMap<Int, Usage>()
            var returnGenericIndex: Usage? = null
            for (line in str.lines()) {
                if (line.isEmpty()) continue
                val eqIdx = line.indexOf('=')
                val key = line.substring(0, eqIdx)
                val usage = Usage.decode(line.substring(eqIdx + 1))
                if (key == "ret") {
                    returnGenericIndex = usage
                } else {
                    parameterGenericIndices[key.toInt()] = usage
                }
            }
            return SpecTypeParametersUsages(parameterGenericIndices, returnGenericIndex)
        }
    }
}
