/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Consult KDoc of [StandardClassIds] regarding naming rules when adding declarations here.
 * Also, check if the desired id is already present there.
 */
object NativeStandardClassIds {
    val BASE_NATIVE_PACKAGE = StandardClassIds.BASE_KOTLIN_PACKAGE.child(Name.identifier("native"))
    val BASE_NATIVE_CONCURRENT_PACKAGE = BASE_NATIVE_PACKAGE.child(Name.identifier("concurrent"))
    val BASE_NATIVE_INTERNAL_PACKAGE = BASE_NATIVE_PACKAGE.child(Name.identifier("internal"))
    val BASE_NATIVE_INTERNAL_ESCAPEANALYSIS_PACKAGE = BASE_NATIVE_INTERNAL_PACKAGE.child(Name.identifier("escapeAnalysis"))
    val BASE_NATIVE_INTERNAL_OBJC_PACKAGE = BASE_NATIVE_INTERNAL_PACKAGE.child(Name.identifier("objc"))

    val AtomicInt_native = "AtomicInt".concurrentId()
    val AtomicLong_native = "AtomicLong".concurrentId()
    val AtomicReference_native = "AtomicReference".concurrentId()

    val atomicByPrimitive_native = mapOf(
        StandardClassIds.Int to AtomicInt_native,
        StandardClassIds.Long to AtomicLong_native,
    )

    val AtomicArray_native = "AtomicArray".concurrentId()
    val AtomicIntArray_native = "AtomicIntArray".concurrentId()
    val AtomicLongArray_native = "AtomicLongArray".concurrentId()

    val atomicArrayByPrimitive_native = mapOf(
        StandardClassIds.Int to AtomicIntArray_native,
        StandardClassIds.Long to AtomicLongArray_native,
    )

    object Annotations {
        val SymbolName = "SymbolName".nativeId()
        val CName = "CName".nativeId()

        val ExportedBridge = "ExportedBridge".nativeInternalId()
        val ImportedBridge = "ImportedBridge".nativeInternalId()
        val ExportForCppRuntime = "ExportForCppRuntime".nativeInternalId()
        val ExportForCompiler = "ExportForCompiler".nativeInternalId()
        val ExportTypeInfo = "ExportTypeInfo".nativeInternalId()
        val GCUnsafeCall = "GCUnsafeCall".nativeInternalId()
        val HasFinalizer = "HasFinalizer".nativeInternalId()

        /** `kotlin.native.Throws`: a typealias to the common `kotlin.Throws` ([StandardClassIds.Annotations.Throws]). */
        val Throws_alias = "Throws".nativeId()

        val SharedImmutable = "SharedImmutable".nativeConcurrentId()
        val SharedImmutable_alias = "SharedImmutable".nativeId()

        val ThreadLocal = "ThreadLocal".nativeConcurrentId()
        val ThreadLocal_alias = "ThreadLocal".nativeId()

        val PointsTo = "PointsTo".nativeInternalEscapeanalysisId()
        val Escapes = "Escapes".nativeInternalEscapeanalysisId()
        val Escapes_Nothing = Escapes.createNestedClassId(Name.identifier("Nothing"))

        val BindClassToObjCName = "BindClassToObjCName".nativeInternalObjcId()
        val BindReverseBridgeToMethod = "BindReverseBridgeToMethod".nativeInternalObjcId()
    }

    object Callables {
        val AtomicReference_compareAndSet_native = "compareAndSet".callableId(AtomicReference_native)
        val AtomicReference_compareAndExchange_native = "compareAndExchange".callableId(AtomicReference_native)
        val AtomicArray_compareAndSet_native = "compareAndSet".callableId(AtomicArray_native)
        val AtomicArray_compareAndExchange_native = "compareAndExchange".callableId(AtomicArray_native)
    }
}

private fun String.concurrentId() = ClassId(StandardClassIds.BASE_CONCURRENT_PACKAGE, Name.identifier(this))
private fun String.nativeId() = ClassId(NativeStandardClassIds.BASE_NATIVE_PACKAGE, Name.identifier(this))
private fun String.nativeConcurrentId() = ClassId(NativeStandardClassIds.BASE_NATIVE_CONCURRENT_PACKAGE, Name.identifier(this))
private fun String.nativeInternalId() = ClassId(NativeStandardClassIds.BASE_NATIVE_INTERNAL_PACKAGE, Name.identifier(this))
private fun String.nativeInternalEscapeanalysisId() =
    ClassId(NativeStandardClassIds.BASE_NATIVE_INTERNAL_ESCAPEANALYSIS_PACKAGE, Name.identifier(this))
private fun String.nativeInternalObjcId() = ClassId(NativeStandardClassIds.BASE_NATIVE_INTERNAL_OBJC_PACKAGE, Name.identifier(this))

private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))
