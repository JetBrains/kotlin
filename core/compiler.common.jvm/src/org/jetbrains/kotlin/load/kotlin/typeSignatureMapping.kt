/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

interface JvmTypeFactory<T : Any> {
    fun boxType(possiblyPrimitiveType: T): T
    fun createFromString(representation: String): T
    fun createPrimitiveType(primitiveType: PrimitiveType): T
    fun createObjectType(internalName: String): T
    fun toString(type: T): String

    val javaLangClassType: T
}

fun <T : Any> JvmTypeFactory<T>.boxTypeIfNeeded(possiblyPrimitiveType: T, needBoxedType: Boolean): T =
    if (needBoxedType) boxType(possiblyPrimitiveType) else possiblyPrimitiveType

const val NON_EXISTENT_CLASS_NAME = "error/NonExistentClass"

fun <T : Any> TypeSystemCommonBackendContext.mapBuiltInType(
    type: KotlinTypeMarker,
    typeFactory: JvmTypeFactory<T>,
    mode: TypeMappingMode
): T? {
    val constructor = type.typeConstructor()
    if (!constructor.isClassTypeConstructor()) return null

    val primitiveType = constructor.getPrimitiveType()
    if (primitiveType != null) {
        val jvmType = typeFactory.createPrimitiveType(primitiveType)
        val isNullableInJava = type.isNullableType() || hasEnhancedNullability(type)
        return typeFactory.boxTypeIfNeeded(jvmType, isNullableInJava)
    }

    val arrayElementType = constructor.getPrimitiveArrayType()
    if (arrayElementType != null) {
        return typeFactory.createFromString("[" + JvmPrimitiveType.get(arrayElementType).desc)
    }

    if (constructor.isUnderKotlinPackage()) {
        val classId = constructor.getClassFqNameUnsafe()?.let(JavaToKotlinClassMap::mapKotlinToJava)
        if (classId != null) {
            if (!mode.kotlinCollectionsToJavaCollections && JavaToKotlinClassMap.mutabilityMappings.any { it.javaClass == classId })
                return null

            return typeFactory.createObjectType(JvmClassName.internalNameByClassId(classId))
        }
    }

    return null
}

open class JvmDescriptorTypeWriter<T : Any>(private val jvmTypeFactory: JvmTypeFactory<T>) {
    private var jvmCurrentTypeArrayLevel: Int = 0
    protected var jvmCurrentType: T? = null
        private set

    protected fun clearCurrentType() {
        jvmCurrentType = null
        jvmCurrentTypeArrayLevel = 0
    }

    open fun writeArrayType() {
        if (jvmCurrentType == null) {
            ++jvmCurrentTypeArrayLevel
        }
    }

    open fun writeArrayEnd() {
    }

    open fun writeClass(objectType: T) {
        writeJvmTypeAsIs(objectType)
    }

    protected fun writeJvmTypeAsIs(type: T) {
        if (jvmCurrentType == null) {
            jvmCurrentType =
                if (jvmCurrentTypeArrayLevel > 0) {
                    jvmTypeFactory.createFromString("[".repeat(jvmCurrentTypeArrayLevel) + jvmTypeFactory.toString(type))
                } else {
                    type
                }
        }
    }

    open fun writeTypeVariable(name: Name, type: T) {
        writeJvmTypeAsIs(type)
    }
}
