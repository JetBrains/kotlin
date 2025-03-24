/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.semantics

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.atMostOne

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrConstructorCall.isAnnotation(name: FqName) = symbol.owner.parentAsClass.fqNameWhenAvailable == name

fun IrAnnotationContainer.getAnnotation(name: FqName): IrConstructorCall? =
    annotations.find { it.isAnnotation(name) }

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrAnnotationContainer.hasAnnotation(name: FqName) =
    annotations.any {
        it.symbol.owner.parentAsClass.hasEqualFqName(name)
    }

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrAnnotationContainer.hasAnnotation(classId: ClassId) =
    annotations.any { it.symbol.owner.parentAsClass.classId == classId }

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrAnnotationContainer.hasAnnotation(symbol: IrClassSymbol) =
    annotations.any {
        it.symbol.owner.parentAsClass.symbol == symbol
    }

fun List<IrConstructorCall>.hasAnnotation(classId: ClassId): Boolean = hasAnnotation(classId.asSingleFqName())

fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
    any { it.annotationClass.hasEqualFqName(fqName) }

fun List<IrConstructorCall>.findAnnotation(fqName: FqName): IrConstructorCall? =
    firstOrNull { it.annotationClass.hasEqualFqName(fqName) }

fun IrConstructorCall.getAnnotationStringValue() = (arguments[0] as? IrConst)?.value as String?

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrConstructorCall.getAnnotationStringValue(name: String): String {
    val parameter = symbol.owner.parameters.single { it.name.asString() == name }
    return (arguments[parameter.indexInParameters] as IrConst).value as String
}

inline fun <reified T> IrConstructorCall.getAnnotationValueOrNull(name: String): T? =
    getAnnotationValueOrNullImpl(name) as T?

@OptIn(UnsafeDuringIrConstructionAPI::class)
@PublishedApi
internal fun IrConstructorCall.getAnnotationValueOrNullImpl(name: String): Any? {
    val parameter = symbol.owner.parameters.atMostOne { it.name.asString() == name }
    val argument = parameter?.let { arguments[it.indexInParameters] }
    return (argument as IrConst?)?.value
}

inline fun <reified T> IrDeclaration.getAnnotationArgumentValue(fqName: FqName, argumentName: String): T? =
    getAnnotationArgumentValueImpl(fqName, argumentName) as T?

@OptIn(UnsafeDuringIrConstructionAPI::class)
@PublishedApi
internal fun IrDeclaration.getAnnotationArgumentValueImpl(fqName: FqName, argumentName: String): Any? {
    val annotation = this.annotations.findAnnotation(fqName) ?: return null
    for (parameter in annotation.symbol.owner.parameters) {
        if (parameter.name.asString() == argumentName) {
            val actual = annotation.arguments[parameter.indexInParameters] as? IrConst
            return actual?.value
        }
    }
    return null
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrClass.getAnnotationRetention(): KotlinRetention? {
    val retentionArgument =
        getAnnotation(StandardNames.FqNames.retention)?.getValueArgument(StandardClassIds.Annotations.ParameterNames.retentionValue)
                as? IrGetEnumValue ?: return null
    val retentionArgumentValue = retentionArgument.symbol.owner
    return KotlinRetention.valueOf(retentionArgumentValue.name.asString())
}

/**
 * Whether this declaration (or its corresponding property if it's a property accessor) has the [PublishedApi] annotation.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrDeclaration.isPublishedApi(): Boolean =
    hasAnnotation(StandardClassIds.Annotations.PublishedApi) ||
            (this as? IrSimpleFunction)
                ?.correspondingPropertySymbol
                ?.owner
                ?.hasAnnotation(StandardClassIds.Annotations.PublishedApi) ?: false

/**
 * @return null - if [this] class is not an annotation class ([isAnnotationClass])
 * set of [org.jetbrains.kotlin.descriptors.annotations.KotlinTarget] representing the annotation targets of the annotation
 * ```
 * @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
 * annotation class Foo
 * ```
 *
 * shall return Class, Function, Property & Constructor
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrClass.getAnnotationTargets(): Set<KotlinTarget>? {
    if (!this.isAnnotationClass) return null

    val valueArgument = getAnnotation(StandardNames.FqNames.target)
        ?.getValueArgument(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets) as? IrVararg
        ?: return KotlinTarget.Companion.DEFAULT_TARGET_SET
    return valueArgument.elements.filterIsInstance<IrGetEnumValue>().mapNotNull {
        KotlinTarget.Companion.valueOrNull(it.symbol.owner.name.asString())
    }.toSet()
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private val IrConstructorCall.annotationClass
    get() = this.symbol.owner.constructedClass

fun IrConstructorCall.isAnnotationWithEqualFqName(fqName: FqName): Boolean =
    annotationClass.hasEqualFqName(fqName)

fun filterOutAnnotations(fqName: FqName, annotations: List<IrConstructorCall>): List<IrConstructorCall> {
    return annotations.filterNot { it.annotationClass.hasEqualFqName(fqName) }
}