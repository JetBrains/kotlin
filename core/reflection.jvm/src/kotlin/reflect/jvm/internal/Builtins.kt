/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClassFinder
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentHashMap
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.common.KotlinCommonMetadata

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

private class BuiltinClassCache(fragment: KmModuleFragment?) {
    val classes: Map<ClassName, KmClass> = fragment?.classes?.associateBy { it.name }.orEmpty()

    companion object {
        val EMPTY = BuiltinClassCache(null)
    }
}

private val builtinClassCaches = ConcurrentHashMap<FqName, BuiltinClassCache>()

internal fun readBuiltinClassMetadata(classId: ClassId): KmClass? {
    val packageFqName = classId.packageFqName
    if (packageFqName !in StandardNames.BUILT_INS_PACKAGE_FQ_NAMES) return null

    val cache = builtinClassCaches.computeIfAbsent(packageFqName) { packageFqName ->
        val inputStream = ReflectKotlinClassFinder(Unit::class.java.safeClassLoader).findBuiltInsData(packageFqName)
            ?: return@computeIfAbsent BuiltinClassCache.EMPTY
        val metadata = KotlinCommonMetadata.read(inputStream)
            ?: throw KotlinReflectionInternalError("Builtins metadata for $packageFqName has unsupported version. Please update kotlin-reflect.")
        BuiltinClassCache(metadata.kmModuleFragment)
    }
    return cache.classes[classId.asString()]
        ?: throw KotlinReflectionInternalError("Builtin class metadata not found for $classId.")
}
