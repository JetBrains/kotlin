/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.Companion.processGenericArguments
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.TypePath

internal class TypePathInfo<T>(
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

internal class IrTypeAnnotationCollector(private val context: JvmBackendContext) {
    private val state: State<IrConstructorCall> = State(arrayListOf())

    fun collectTypeAnnotations(kotlinType: IrType, mode: TypeMappingMode): List<TypePathInfo<IrConstructorCall>> {
        kotlinType.gatherTypeAnnotations(mode)
        return state.results
    }

    private fun IrType.gatherTypeAnnotations(typeMappingMode: TypeMappingMode) {
        if (classOrNull?.owner?.isInner == true) {
            // Skip inner classes for now. It's not clear whether type annotations on outer class should be supported.
            return
        }

        extractAnnotations().ifNotEmpty(state::rememberAnnotations)

        if (this !is IrSimpleType) return

        context.typeSystem.processGenericArguments(
            arguments,
            classOrNull?.owner?.typeParameters?.map { it.symbol }.orEmpty(),
            typeMappingMode,
            processUnboundedWildcard = {
                // There's no syntax in Kotlin to annotate a wildcard.
            },
            processTypeArgument = { index, type, projectionKind, _, newMode ->
                if (isArray() || isNullableArray()) {
                    state.addStep("[")
                } else {
                    state.addStep("$index;")
                    if (projectionKind != Variance.INVARIANT) {
                        state.addStep("*")
                    }
                }
                (type as IrType).gatherTypeAnnotations(newMode)
                state.removeStep()
            },
        )
    }

    private fun IrType.extractAnnotations(): List<IrConstructorCall> {
        return annotations.filter {
            val annotationClass = it.symbol.owner.parentAsClass

            // We only generate annotations which have the TYPE_USE Java target.
            // Those are type annotations which were compiled with JVM target bytecode version 1.8 or greater
            (annotationClass.origin != IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
                    annotationClass.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) ||
                    annotationClass.isCompiledToJvm8OrHigher
        }
    }

    private fun isCompiledToJvm8OrHigher(source: SourceElement): Boolean =
        source !is KotlinJvmBinarySourceElement ||
                ((source.binaryClass as? FileBasedKotlinClass)?.classVersion ?: 0) >= Opcodes.V1_8

    private val IrClass.isCompiledToJvm8OrHigher: Boolean
        get() =
            (origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB || origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) &&
                    isCompiledToJvm8OrHigher(source)
}
