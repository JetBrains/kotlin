/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAnnotationNames

internal val repeatedAnnotationPhase = makeIrModulePhase(
    ::RepeatedAnnotationLowering,
    name = "RepeatedAnnotation",
    description = "Enclose repeated annotations in a container annotation, generating a container class if needed"
)

class RepeatedAnnotationLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementVisitorVoid {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        declaration.annotations = transformAnnotations(declaration.annotations)
        super.visitFile(declaration)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        declaration.annotations = transformAnnotations(declaration.annotations)
        super.visitDeclaration(declaration)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.kind == ClassKind.ANNOTATION_CLASS &&
            declaration.hasAnnotation(StandardNames.FqNames.repeatable) &&
            !declaration.hasAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION)
        ) {
            declaration.declarations.add(context.cachedDeclarations.getRepeatedAnnotationSyntheticContainer(declaration))
        }
        super.visitClass(declaration)
    }

    private fun transformAnnotations(annotations: List<IrConstructorCall>): List<IrConstructorCall> {
        if (annotations.size < 2) return annotations

        val annotationsByClass = annotations.groupByTo(mutableMapOf()) { it.symbol.owner.constructedClass }
        if (annotationsByClass.values.none { it.size > 1 }) return annotations

        val result = mutableListOf<IrConstructorCall>()
        for (annotation in annotations) {
            val annotationClass = annotation.symbol.owner.constructedClass
            val grouped = annotationsByClass.remove(annotationClass) ?: continue
            if (grouped.size < 2) {
                result.add(grouped.single())
                continue
            }

            val containerClass = getOrCreateContainerClass(annotationClass)
            result.add(wrapAnnotationEntriesInContainer(annotationClass, containerClass, grouped))
        }
        return result
    }

    private fun getOrCreateContainerClass(annotationClass: IrClass): IrClass {
        val metaAnnotations = annotationClass.annotations
        val jvmRepeatable = metaAnnotations.find { it.isAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION) }
        return if (jvmRepeatable != null) {
            val containerClassReference = jvmRepeatable.getValueArgument(0)
            require(containerClassReference is IrClassReference) {
                "Repeatable annotation container value must be a class reference: $annotationClass"
            }
            (containerClassReference.symbol as? IrClassSymbol)?.owner
                ?: error("Repeatable annotation container must be a class: $annotationClass")
        } else {
            context.cachedDeclarations.getRepeatedAnnotationSyntheticContainer(annotationClass)
        }
    }

    private fun wrapAnnotationEntriesInContainer(
        annotationClass: IrClass,
        containerClass: IrClass,
        entries: List<IrConstructorCall>,
    ): IrConstructorCall {
        val annotationType = annotationClass.typeWith()
        return IrConstructorCallImpl.fromSymbolOwner(containerClass.defaultType, containerClass.primaryConstructor!!.symbol).apply {
            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.arrayClass.typeWith(annotationType),
                    annotationType,
                    entries
                )
            )
        }
    }
}
