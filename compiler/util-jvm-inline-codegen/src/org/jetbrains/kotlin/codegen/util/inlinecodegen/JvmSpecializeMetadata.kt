/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.String
import kotlin.collections.List

data class JvmSpecializeMetadataValue(
    val specializedSlots: List<Int>,
    val specTypeParametersUsages: SpecTypeParametersUsages,
    val typeParametersNames: List<String>,
) {
    companion object {
        const val ANNOTATION_DESCRIPTOR_NAME = "Lkotlin/jvm/JvmSpecializeMetadata;"
        const val SPECIALIZED_SLOTS_NAME = "specializedSlots"
        const val SPEC_TYPE_PARAMETERS_USAGE_NAME = "specTypeParametersUsages"
        const val TYPE_PARAMETERS_NAMES_NAME = "typeParametersNames"
    }

    fun toAnnotationNode(): AnnotationNode {
        return AnnotationNode("Lkotlin/jvm/JvmSpecializeMetadata;").apply {
            values = listOf(
                SPECIALIZED_SLOTS_NAME, specializedSlots,
                SPEC_TYPE_PARAMETERS_USAGE_NAME, specTypeParametersUsages.encode(),
                TYPE_PARAMETERS_NAMES_NAME, typeParametersNames,
            )
        }
    }

    fun emitAnnotation(mv: MethodVisitor) {
        mv.visitAnnotation(ANNOTATION_DESCRIPTOR_NAME, false)?.let { av ->
            av.visit(SPECIALIZED_SLOTS_NAME, specializedSlots)
            av.visit(SPEC_TYPE_PARAMETERS_USAGE_NAME, specTypeParametersUsages.encode())
            av.visit(TYPE_PARAMETERS_NAMES_NAME, typeParametersNames)
            av.visitEnd()
        }
    }
}

fun MethodNode.extractJvmSpecializeMetadataValue(): JvmSpecializeMetadataValue? {
    val annotation = invisibleAnnotations?.find { it.desc == JvmSpecializeMetadataValue.ANNOTATION_DESCRIPTOR_NAME } ?: return null

    fun getValue(valueName: String) =
        annotation.values.indexOf(valueName)
            .takeIf { it != -1 }
            ?.let { annotation.values[it + 1] }
            ?: error("invalid JvmSpecializeMetadata: missing '$valueName'")

    @Suppress("UNCHECKED_CAST")
    return JvmSpecializeMetadataValue(
        getValue(JvmSpecializeMetadataValue.SPECIALIZED_SLOTS_NAME) as List<Int>,
        SpecTypeParametersUsages.decode(getValue(JvmSpecializeMetadataValue.SPEC_TYPE_PARAMETERS_USAGE_NAME) as String),
        getValue(JvmSpecializeMetadataValue.TYPE_PARAMETERS_NAMES_NAME) as List<String>,
    )
}
