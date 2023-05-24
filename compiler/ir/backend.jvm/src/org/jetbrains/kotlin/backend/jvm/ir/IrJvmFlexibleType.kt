/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.FlexibleTypeBoundsChecker
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker

internal interface IrJvmFlexibleType : FlexibleTypeMarker {
    val lowerBound: IrSimpleType
    val upperBound: IrSimpleType
}

private class IrJvmFlexibleTypeImpl(
    val irType: IrSimpleType,
    val builtIns: IrBuiltIns,
    val nullability: Boolean,
    val mutability: Boolean,
    val raw: Boolean,
) : IrJvmFlexibleType {
    override val lowerBound: IrSimpleType
        get() = irType.buildSimpleType {
            if (this@IrJvmFlexibleTypeImpl.nullability) nullability = SimpleTypeNullability.NOT_SPECIFIED
            // No change in classifier is needed for mutability because type's classifier is set to the lower bound anyway
            // (see TypeTranslator.translateType).
            kotlinType = null
            if (mutability) {
                val klass = classifier?.owner as? IrClass
                    ?: error("Mutability-flexible type's classifier is not a class: ${irType.render()}")
                val readonlyClassFqName = FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(klass.fqNameWhenAvailable!!)
                classifier = when (readonlyClassFqName) {
                    StandardNames.FqNames.iterable -> builtIns.mutableIterableClass
                    StandardNames.FqNames.iterator -> builtIns.mutableIteratorClass
                    StandardNames.FqNames.listIterator -> builtIns.mutableListIteratorClass
                    StandardNames.FqNames.list -> builtIns.mutableListClass
                    StandardNames.FqNames.collection -> builtIns.mutableCollectionClass
                    StandardNames.FqNames.set -> builtIns.mutableSetClass
                    StandardNames.FqNames.map -> builtIns.mutableMapClass
                    StandardNames.FqNames.mapEntry -> builtIns.mutableMapEntryClass
                    else -> error("Mutability-flexible type with unknown classifier: ${irType.render()}, FQ name: $readonlyClassFqName")
                }
            }

        }

    override val upperBound: IrSimpleType
        get() = irType.buildSimpleType {
            if (this@IrJvmFlexibleTypeImpl.nullability) nullability = SimpleTypeNullability.MARKED_NULLABLE
            if (mutability) {
                val klass = classifier?.owner as? IrClass
                    ?: error("Mutability-flexible type's classifier is not a class: ${irType.render()}")
                val readonlyClassFqName = FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(klass.fqNameWhenAvailable!!)
                classifier = when (readonlyClassFqName) {
                    StandardNames.FqNames.iterable -> builtIns.iterableClass
                    StandardNames.FqNames.iterator -> builtIns.iteratorClass
                    StandardNames.FqNames.listIterator -> builtIns.listIteratorClass
                    StandardNames.FqNames.list -> builtIns.listClass
                    StandardNames.FqNames.collection -> builtIns.collectionClass
                    StandardNames.FqNames.set -> builtIns.setClass
                    StandardNames.FqNames.map -> builtIns.mapClass
                    StandardNames.FqNames.mapEntry -> builtIns.mapEntryClass
                    else -> error("Mutability-flexible type with unknown classifier: ${irType.render()}, FQ name: $readonlyClassFqName")
                }
            }
            if (raw) {
                arguments = List(arguments.size) { IrStarProjectionImpl }
            }
            kotlinType = null
        }
}

fun IrType.isWithFlexibleNullability(): Boolean =
    hasAnnotation(JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME)

internal fun IrType.isWithFlexibleMutability(): Boolean =
    hasAnnotation(JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME)

internal fun IrType.asJvmFlexibleType(builtIns: IrBuiltIns): FlexibleTypeMarker? {
    if (this !is IrSimpleType || annotations.isEmpty()) return null

    val nullability = isWithFlexibleNullability()
    val mutability = isWithFlexibleMutability()
    val raw = isRawType()
    if (!nullability && !mutability && !raw) return null

    val baseType = this.removeAnnotations { irCtorCall ->
        val fqName = irCtorCall.type.classFqName
        fqName == JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME ||
                fqName == JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME ||
                fqName == JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME
    } as IrSimpleType

    return IrJvmFlexibleTypeImpl(baseType, builtIns, nullability, mutability, raw)
}
