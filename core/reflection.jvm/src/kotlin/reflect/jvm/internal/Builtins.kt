/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClassFinder
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.common.KotlinCommonMetadata
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.MutableCollectionKClassImpl

internal fun createFunctionKmClass(arity: Int): KmClass = KmClass().apply {
    name = "kotlin/Function$arity"
    kind = ClassKind.INTERFACE
    modality = Modality.ABSTRACT
    visibility = Visibility.PUBLIC

    for (i in 1..arity) {
        typeParameters.add(KmTypeParameter("P$i", i, KmVariance.IN))
    }
    val returnTypeParameterId = arity + 1
    typeParameters.add(KmTypeParameter("R", returnTypeParameterId, KmVariance.OUT))

    supertypes.add(KmType().apply {
        classifier = KmClassifier.Class("kotlin/Function")
        arguments.add(KmTypeProjection(KmVariance.INVARIANT, KmType().apply {
            classifier = KmClassifier.TypeParameter(returnTypeParameterId)
        }))
    })

    // TODO (KT-80710): `invoke` function.
}

internal fun createCloneableKmClass(): KmClass = KmClass().apply {
    name = "kotlin/Cloneable"
    kind = ClassKind.INTERFACE
    modality = Modality.ABSTRACT
    visibility = Visibility.PUBLIC

    functions.add(KmFunction("clone").apply {
        modality = Modality.OPEN
        visibility = Visibility.PROTECTED
        returnType = KmType().apply {
            classifier = KmClassifier.Class("kotlin/Any")
        }
    })
}

private class BuiltinClassCache(fragment: KmModuleFragment?) {
    val classes: Map<ClassName, KmClass> = fragment?.classes?.associateBy { it.name }.orEmpty()

    companion object {
        val EMPTY = BuiltinClassCache(null)
    }
}

private val builtinClassCaches = ConcurrentHashMap<FqName, SoftReference<BuiltinClassCache>>()

internal fun readBuiltinClassMetadata(classId: ClassId): KmClass? {
    val packageFqName = classId.packageFqName
    if (packageFqName !in StandardNames.BUILT_INS_PACKAGE_FQ_NAMES) return null

    val cache = builtinClassCaches[packageFqName]?.get() ?: run {
        val inputStream = ReflectKotlinClassFinder(Unit::class.java.safeClassLoader).findBuiltInsData(packageFqName)
            ?: return@run BuiltinClassCache.EMPTY
        val metadata = KotlinCommonMetadata.read(inputStream)
            ?: throw KotlinReflectionInternalError("Builtins metadata for $packageFqName has unsupported version. Please update kotlin-reflect.")
        BuiltinClassCache(metadata.kmModuleFragment).also {
            builtinClassCaches[packageFqName] = SoftReference(it)
        }
    }
    return cache.classes[classId.asString()]
        ?: throw KotlinReflectionInternalError("Builtin class metadata not found for $classId.")
}

private val mutableCollectionKClassCache = ConcurrentHashMap<ClassId, MutableCollectionKClass<*>>()

internal fun getMutableCollectionKClass(readonlyClass: KClass<*>): MutableCollectionKClass<*>? {
    val readOnlyClassId = (readonlyClass as? KClassImpl<*>)?.classId ?: return null
    val mutableClassId = JavaToKotlinClassMap.readOnlyToMutable(readOnlyClassId) ?: return null
    return mutableCollectionKClassCache[mutableClassId]
        ?: MutableCollectionKClassImpl(readonlyClass, mutableClassId).let {
            mutableCollectionKClassCache.putIfAbsent(mutableClassId, it) ?: it
        }
}

internal fun clearBuiltinClassCaches() {
    builtinClassCaches.clear()
    mutableCollectionKClassCache.clear()
}
