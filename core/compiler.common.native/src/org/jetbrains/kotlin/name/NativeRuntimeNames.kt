/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import java.util.concurrent.atomic.AtomicReference

object NativeRuntimeNames {
    private val kotlinNativePackage = FqName("kotlin.native")
    private val kotlinNativeInternalPackage = kotlinNativePackage.child(Name.identifier("internal"))

    val AtomicInt = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier("AtomicInt"))
    val AtomicLong = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier("AtomicLong"))
    val AtomicReference = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier("AtomicReference"))

    val atomicByPrimitive = mapOf(
        StandardClassIds.Int to AtomicInt,
        StandardClassIds.Long to AtomicLong,
    )

    val AtomicArray = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier("AtomicArray"))
    val AtomicIntArray = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier("AtomicIntArray"))
    val AtomicLongArray = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier("AtomicLongArray"))

    val atomicArrayByPrimitive = mapOf(
        StandardClassIds.Int to AtomicIntArray,
        StandardClassIds.Long to AtomicLongArray,
    )

    object Callables {
        val AtomicArray = "AtomicArray".callableId(StandardClassIds.BASE_CONCURRENT_PACKAGE)

        val atomicReferenceCompareAndSet = "compareAndSet".callableId(AtomicReference)
        val atomicReferenceCompareAndExchange = "compareAndExchange".callableId(AtomicReference)
        val atomicArrayCompareAndSet = "compareAndSet".callableId(NativeRuntimeNames.AtomicArray)
        val atomicArrayCompareAndExchange = "compareAndExchange".callableId(NativeRuntimeNames.AtomicArray)
    }

    object Annotations {
        val symbolNameClassId = ClassId(kotlinNativePackage, Name.identifier("SymbolName"))
        val cNameClassId = ClassId(kotlinNativePackage, Name.identifier("CName"))
        val exportedBridgeClassId = ClassId(kotlinNativeInternalPackage, Name.identifier("ExportedBridge"))
        val exportForCppRuntimeClassId = ClassId(kotlinNativeInternalPackage, Name.identifier("ExportForCppRuntime"))
        val exportForCompilerClassId = ClassId(kotlinNativeInternalPackage, Name.identifier("ExportForCompiler"))
        val exportTypeInfoClassId = ClassId(kotlinNativeInternalPackage, Name.identifier("ExportTypeInfo"))
        val gcUnsafeCallClassId = ClassId(kotlinNativeInternalPackage, Name.identifier("GCUnsafeCall"))
        val Throws = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("Throws"))
        val ThrowsAlias = ClassId(kotlinNativePackage, Name.identifier("Throws"))
        val SharedImmutable = ClassId(kotlinNativePackage.child(Name.identifier("concurrent")), Name.identifier("SharedImmutable"))
        val SharedImmutableAlias = ClassId(kotlinNativePackage, Name.identifier("SharedImmutable"))
        val ThreadLocal = ClassId(kotlinNativePackage.child(Name.identifier("concurrent")), Name.identifier("ThreadLocal"))
        val ThreadLocalAlias = ClassId(kotlinNativePackage, Name.identifier("ThreadLocal"))
        val PointsTo = ClassId(kotlinNativeInternalPackage.child(Name.identifier("escapeAnalysis")), Name.identifier("PointsTo"))
        val Escapes = ClassId(kotlinNativeInternalPackage.child(Name.identifier("escapeAnalysis")), Name.identifier("Escapes"))
        val EscapesNothing = Escapes.createNestedClassId(Name.identifier("Nothing"))
        val HasFinalizer = ClassId(kotlinNativeInternalPackage, Name.identifier("HasFinalizer"))
        val BindClassToObjCName = ClassId(kotlinNativeInternalPackage.child(Name.identifier("objc")), Name.identifier("BindClassToObjCName"))
    }
}

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))
