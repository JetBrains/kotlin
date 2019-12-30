/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.TypePath


class TypePathInfo(
    val path: TypePath?,
    val annotations: List<AnnotationDescriptor>
)

private class State(val type: Int, val path: MutableList<String>) {

    val results = arrayListOf<TypePathInfo>()


    fun addStep(step: String) {
        path.add(step)
    }

    fun removeStep(step: String) {
        path.removeAt(path.lastIndex)
    }

    fun rememberAnnotations(annotations: List<AnnotationDescriptor>) {
        results.add(TypePathInfo(TypePath.fromString(path.joinToString("")), annotations))
    }
}

class TypeAnnotationCollector {

    private lateinit var state: State

    fun collectTypeAnnotations(kotlinType: KotlinType, annotationType: Int): ArrayList<TypePathInfo> {
        state = State(annotationType, arrayListOf())
        kotlinType.collectTypeAnnotations()
        return state.results
    }

    private fun KotlinType.collectTypeAnnotations() {
        if (isFlexible()) {
            return upperIfFlexible().collectTypeAnnotations()
        } else if ((this.constructor.declarationDescriptor as? ClassDescriptor)?.isInner == true) {
            //skip inner classes for now it's not clear should type annotations on outer be supported or not
            return
        }

        typeAnnotations.takeIf { it.isNotEmpty() }?.let { state.rememberAnnotations(it) }

        arguments.forEachIndexed { index, type ->
            //skip in/out variance for now it's not clear should type annotations on wildcard bound be supported or not
            if (type.projectionKind == Variance.INVARIANT) {
                when {
                    KotlinBuiltIns.isArray(this) -> type.type.process("[")
                    else -> type.type.process("$index;")
                }
            }
        }
    }

    fun KotlinType.process(step: String) {
        state.addStep(step)
        this.collectTypeAnnotations()
        state.removeStep(step)
    }


    private val KotlinType.typeAnnotations
        get() = annotations.filter {
            //We only generate annotations which have the TYPE_USE Java target.
            // Those are type annotations which were compiled with JVM target bytecode version 1.8 or greater
            (it.annotationClass as? DeserializedClassDescriptor)?.let { classDescriptor ->
                ((classDescriptor.source as? KotlinJvmBinarySourceElement)?.binaryClass as? FileBasedKotlinClass)?.classVersion ?: 0 >= Opcodes.V1_8
            } ?: true
        }

}