/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.kotlinpoet

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun arrayTypeNameOf(typeParameter: TypeName): ParameterizedTypeName = Array::class.asTypeName().parameterizedBy(typeParameter)
fun listTypeNameOf(typeParameter: TypeName): ParameterizedTypeName = List::class.asTypeName().parameterizedBy(typeParameter)

inline fun <reified T> arrayTypeNameOf(): ParameterizedTypeName = Array::class.parameterizedBy(T::class)
inline fun <reified T> listTypeNameOf(): ParameterizedTypeName = List::class.parameterizedBy(T::class)

inline fun TypeSpec.Builder.function(name: String, funSpec: FunSpec.Builder.() -> Unit): TypeSpec.Builder = apply {
    addFunction(
        FunSpec.builder(name).apply(funSpec).build()
    )
}

inline fun TypeSpec.Builder.property(
    name: String,
    typeName: TypeName,
    vararg modifiers: KModifier,
    propertySpec: PropertySpec.Builder.() -> Unit = {},
): PropertySpec =
    PropertySpec.builder(name, typeName, *modifiers).apply(propertySpec).build().also { addProperty(it) }

/**
 * @param T type of the property. If `T` is generic itself then its parameters will be erased at runtime.
 * Use `TypeSpec.Builder.property(String, TypeName, ...)`]` and [ClassName.parameterizedBy] for such cases.
 *
 * @see listTypeNameOf
 * @see arrayTypeNameOf
 */
inline fun <reified T> TypeSpec.Builder.property(
    name: String, vararg modifiers: KModifier, propertySpec: PropertySpec.Builder.() -> Unit = {},
): PropertySpec = property(name, T::class.asTypeName(), *modifiers, propertySpec = propertySpec)

inline fun <reified T : Annotation> PropertySpec.Builder.annotation(
    annotationSpec: AnnotationSpec.Builder.() -> Unit = {},
): PropertySpec.Builder = annotation(T::class.asTypeName(), annotationSpec)

inline fun PropertySpec.Builder.annotation(typeName: ClassName, annotationSpec: AnnotationSpec.Builder.() -> Unit): PropertySpec.Builder =
    addAnnotation(AnnotationSpec.builder(typeName).apply(annotationSpec).build())

inline fun <reified T : Annotation> FunSpec.Builder.annotation(
    annotationSpec: AnnotationSpec.Builder.() -> Unit = {},
): FunSpec.Builder = annotation(T::class.asTypeName(), annotationSpec)


inline fun FunSpec.Builder.annotation(typeName: ClassName, annotationSpec: AnnotationSpec.Builder.() -> Unit = {}): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(typeName).apply(annotationSpec).build())