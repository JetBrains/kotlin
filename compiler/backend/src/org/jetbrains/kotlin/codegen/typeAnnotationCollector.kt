/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.TypePath


class TypePathInfo<T>(
    val path: TypePath?,
    val annotations: List<T>
)

private class State<T>(val path: MutableList<String>) {

    val results = arrayListOf<TypePathInfo<T>>()


    fun addStep(step: String) {
        path.add(step)
    }

    fun removeStep() {
        path.removeAt(path.lastIndex)
    }

    fun rememberAnnotations(annotations: List<T>) {
        results.add(TypePathInfo(TypePath.fromString(path.joinToString("")), annotations))
    }
}

class PsiTypeAnnotationCollector : TypeAnnotationCollector<AnnotationDescriptor>(SimpleClassicTypeSystemContext) {

    override fun KotlinTypeMarker.extractAnnotations(): List<AnnotationDescriptor> {
        require(this is KotlinType)
        return annotations.filter {
            //We only generate annotations which have the TYPE_USE Java target.
            // Those are type annotations which were compiled with JVM target bytecode version 1.8 or greater
            isCompiledToJvm8OrHigher(it.annotationClass)
        }
    }
}

abstract class TypeAnnotationCollector<T>(val context: TypeSystemCommonBackendContext) {

    private lateinit var state: State<T>

    fun collectTypeAnnotations(kotlinType: KotlinTypeMarker): ArrayList<TypePathInfo<T>> {
        state = State(arrayListOf())
        kotlinType.gatherTypeAnnotations()
        return state.results
    }

    private fun KotlinTypeMarker.gatherTypeAnnotations() {
        with(context) {
            if (isFlexible()) {
                return upperBoundIfFlexible().gatherTypeAnnotations()
            } else if (typeConstructor().isInnerClass()) {
                //skip inner classes for now it's not clear should type annotations on outer be supported or not
                return
            }

            extractAnnotations().takeIf { it.isNotEmpty() }?.let { state.rememberAnnotations(it) }

            for (index in 0 until argumentsCount()) {
                val type = getArgument(index)
                //skip in/out variance for now it's not clear should type annotations on wildcard bound be supported or not
                if (type.getVariance() == TypeVariance.INV) {
                    when {
                        this@gatherTypeAnnotations.isArrayOrNullableArray() -> type.getType().process("[")
                        else -> type.getType().process("$index;")
                    }
                }
            }
        }
    }

    fun KotlinTypeMarker.process(step: String) {
        state.addStep(step)
        this.gatherTypeAnnotations()
        state.removeStep()
    }


    abstract fun KotlinTypeMarker.extractAnnotations(): List<T>

    fun isCompiledToJvm8OrHigher(descriptor: ClassDescriptor?): Boolean =
        (descriptor as? DeserializedClassDescriptor)?.let { classDescriptor -> isCompiledToJvm8OrHigher(classDescriptor.source) }
            ?: true

    fun isCompiledToJvm8OrHigher(source: SourceElement): Boolean =
        (source !is KotlinJvmBinarySourceElement ||
                (source.binaryClass as? FileBasedKotlinClass)?.classVersion ?: 0 >= Opcodes.V1_8)
}