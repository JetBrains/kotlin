/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.get

abstract class InlineClassesSupport<Class : Any, Type : Any> {
    abstract fun isNullable(type: Type): Boolean

    abstract fun makeNullable(type: Type): Type
    protected abstract fun erase(type: Type): Class
    protected abstract fun computeFullErasure(type: Type): Sequence<Class>
    protected abstract fun hasInlineModifier(clazz: Class): Boolean
    protected abstract fun getNativePointedSuperclass(clazz: Class): Class?

    abstract fun getInlinedClassUnderlyingType(clazz: Class): Type
    protected abstract fun getPackageFqName(clazz: Class): FqName?
    protected abstract fun getName(clazz: Class): Name?
    abstract fun isTopLevelClass(clazz: Class): Boolean

    @JvmName("classIsInlined")
    fun isInlined(clazz: Class): Boolean = getInlinedClass(clazz) != null
    fun isInlined(type: Type): Boolean = getInlinedClass(type) != null

    fun isUsedAsBoxClass(clazz: Class) = getInlinedClass(clazz) == clazz // To handle NativePointed subclasses.

    fun getInlinedClass(type: Type): Class? =
        getInlinedClass(erase(type), isNullable(type))

    fun getKonanPrimitiveType(clazz: Class): KonanPrimitiveType? =
        if (isTopLevelClass(clazz))
            KonanPrimitiveType.byFqNameParts[getPackageFqName(clazz)]?.get(getName(clazz))
        else null

    fun isImplicitInlineClass(clazz: Class): Boolean =
        isTopLevelClass(clazz) && (getKonanPrimitiveType(clazz) != null ||
                getName(clazz) == KonanFqNames.nativePtr.shortName() && getPackageFqName(clazz) == KonanFqNames.internalPackageName ||
                getName(clazz) == InteropFqNames.cPointer.shortName() && getPackageFqName(clazz) == InteropFqNames.cPointer.parent()
            .toSafe())

    private fun getInlinedClass(erased: Class, isNullable: Boolean): Class? {
        val inlinedClass = getInlinedClass(erased) ?: return null
        return if (!isNullable || representationIsNonNullReferenceOrPointer(inlinedClass)) {
            inlinedClass
        } else {
            null
        }
    }

    tailrec fun representationIsNonNullReferenceOrPointer(clazz: Class): Boolean {
        val konanPrimitiveType = getKonanPrimitiveType(clazz)
        if (konanPrimitiveType != null) {
            return konanPrimitiveType == KonanPrimitiveType.NON_NULL_NATIVE_PTR
        }

        val inlinedClass = getInlinedClass(clazz) ?: return true

        val underlyingType = getInlinedClassUnderlyingType(inlinedClass)
        return if (isNullable(underlyingType)) {
            false
        } else {
            representationIsNonNullReferenceOrPointer(erase(underlyingType))
        }
    }

    @JvmName("classGetInlinedClass")
    private fun getInlinedClass(clazz: Class): Class? =
        if (hasInlineModifier(clazz) || isImplicitInlineClass(clazz)) {
            clazz
        } else {
            getNativePointedSuperclass(clazz)
        }

    inline fun <R> unwrapToPrimitiveOrReference(
        type: Type,
        eachInlinedClass: (inlinedClass: Class, nullable: Boolean) -> Unit,
        ifPrimitive: (primitiveType: KonanPrimitiveType, nullable: Boolean) -> R,
        ifReference: (type: Type) -> R,
    ): R {
        var currentType: Type = type

        while (true) {
            val inlinedClass = getInlinedClass(currentType)
            if (inlinedClass == null) {
                return ifReference(currentType)
            }

            val nullable = isNullable(currentType)

            getKonanPrimitiveType(inlinedClass)?.let { primitiveType ->
                return ifPrimitive(primitiveType, nullable)
            }

            eachInlinedClass(inlinedClass, nullable)

            val underlyingType = getInlinedClassUnderlyingType(inlinedClass)
            currentType = if (nullable) makeNullable(underlyingType) else underlyingType
        }
    }

    fun representationIsNullable(type: Type): Boolean {
        unwrapToPrimitiveOrReference(
            type,
            eachInlinedClass = { _, nullable -> if (nullable) return true },
            ifPrimitive = { _, nullable -> return nullable },
            ifReference = { return isNullable(it) }
        )
    }

    // TODO: optimize.
    fun computeBinaryType(type: Type): BinaryType<Class> {
        val erased = erase(type)
        val inlinedClass = getInlinedClass(erased, isNullable(type)) ?: return createReferenceBinaryType(type)

        getKonanPrimitiveType(inlinedClass)?.let {
            return it.binaryType
        }

        val underlyingBinaryType = computeBinaryType(getInlinedClassUnderlyingType(inlinedClass))
        return if (isNullable(type) && underlyingBinaryType is BinaryType.Reference) {
            BinaryType.Reference(underlyingBinaryType.types, true)
        } else {
            underlyingBinaryType
        }
    }

    private fun createReferenceBinaryType(type: Type): BinaryType.Reference<Class> =
        BinaryType.Reference(computeFullErasure(type), true)
}