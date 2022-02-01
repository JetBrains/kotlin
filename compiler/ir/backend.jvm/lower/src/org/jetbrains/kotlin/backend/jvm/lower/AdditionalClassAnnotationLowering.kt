/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.getAnnotationRetention
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name
import java.lang.annotation.ElementType

internal class AdditionalClassAnnotationLowering(private val context: JvmBackendContext) : ClassLoweringPass {
    private val symbols = context.ir.symbols.javaAnnotations

    override fun lower(irClass: IrClass) {
        if (!irClass.isAnnotationClass) return

        generateDocumentedAnnotation(irClass)
        generateRetentionAnnotation(irClass)
        generateTargetAnnotation(irClass)
        generateRepeatableAnnotation(irClass)
    }

    private fun generateDocumentedAnnotation(irClass: IrClass) {
        if (!irClass.hasAnnotation(StandardNames.FqNames.mustBeDocumented) ||
            irClass.hasAnnotation(JvmAnnotationNames.DOCUMENTED_ANNOTATION)
        ) return

        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.documentedConstructor.returnType, symbols.documentedConstructor.symbol, 0
            )
    }

    private fun generateRetentionAnnotation(irClass: IrClass) {
        if (irClass.hasAnnotation(JvmAnnotationNames.RETENTION_ANNOTATION)) return
        val kotlinRetentionPolicy = irClass.getAnnotationRetention()
        val javaRetentionPolicy = kotlinRetentionPolicy?.let { symbols.annotationRetentionMap[it] } ?: symbols.rpRuntime

        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.retentionConstructor.returnType, symbols.retentionConstructor.symbol, 0
            ).apply {
                putValueArgument(
                    0,
                    IrGetEnumValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.retentionPolicyEnum.defaultType, javaRetentionPolicy.symbol
                    )
                )
            }
    }

    private fun generateTargetAnnotation(irClass: IrClass) {
        if (irClass.hasAnnotation(JvmAnnotationNames.TARGET_ANNOTATION)) return
        val annotationTargetMap = symbols.getAnnotationTargetMap(context.state.target)

        val targets = irClass.applicableTargetSet() ?: return
        val javaTargets = targets.mapNotNullTo(HashSet()) { annotationTargetMap[it] }.sortedBy {
            ElementType.valueOf(it.symbol.owner.name.asString())
        }

        val vararg = IrVarargImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = context.irBuiltIns.arrayClass.typeWith(symbols.elementTypeEnum.defaultType),
            varargElementType = symbols.elementTypeEnum.defaultType
        )
        for (target in javaTargets) {
            vararg.elements.add(
                IrGetEnumValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.elementTypeEnum.defaultType, target.symbol
                )
            )
        }

        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.targetConstructor.returnType, symbols.targetConstructor.symbol, 0
            ).apply {
                putValueArgument(0, vararg)
            }
    }

    private fun generateRepeatableAnnotation(irClass: IrClass) {
        if (!irClass.hasAnnotation(StandardNames.FqNames.repeatable) ||
            irClass.hasAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION)
        ) return

        val containerClass =
            irClass.declarations.singleOrNull {
                it is IrClass && it.name.asString() == JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
            } as IrClass? ?: error("Repeatable annotation class should have a container generated: ${irClass.render()}")
        val containerReference = IrClassReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.kClassClass.typeWith(containerClass.defaultType),
            containerClass.symbol, containerClass.defaultType
        )
        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.repeatableConstructor.returnType, symbols.repeatableConstructor.symbol, 0
            ).apply {
                putValueArgument(0, containerReference)
            }
    }

    private fun IrConstructorCall.getValueArgument(name: Name): IrExpression? {
        val index = symbol.owner.valueParameters.find { it.name == name }?.index ?: return null
        return getValueArgument(index)
    }

    private fun IrClass.applicableTargetSet(): Set<KotlinTarget>? {
        val targetEntry = getAnnotation(StandardNames.FqNames.target) ?: return null
        return loadAnnotationTargets(targetEntry)
    }

    private fun loadAnnotationTargets(targetEntry: IrConstructorCall): Set<KotlinTarget>? {
        val valueArgument = targetEntry.getValueArgument(Name.identifier(Target::allowedTargets.name))
                as? IrVararg ?: return null
        return valueArgument.elements.filterIsInstance<IrGetEnumValue>().mapNotNull {
            KotlinTarget.valueOrNull(it.symbol.owner.name.asString())
        }.toSet()
    }
}
