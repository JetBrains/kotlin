package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeRuntimeNames

object RuntimeNames {
    val symbolNameAnnotation = NativeRuntimeNames.Annotations.symbolNameClassId.asSingleFqName()
    val cnameAnnotation = NativeRuntimeNames.Annotations.cNameClassId.asSingleFqName()
    val exportForCppRuntime = NativeRuntimeNames.Annotations.exportForCppRuntimeClassId.asSingleFqName()
    val exportedBridge = NativeRuntimeNames.Annotations.exportedBridgeClassId.asSingleFqName()
    val exportTypeInfoAnnotation = NativeRuntimeNames.Annotations.exportTypeInfoClassId.asSingleFqName()
    val cCall = FqName("kotlinx.cinterop.internal.CCall")
    val cStructMemberAt = FqName("kotlinx.cinterop.internal.CStruct.MemberAt")
    val cStructArrayMemberAt = FqName("kotlinx.cinterop.internal.CStruct.ArrayMemberAt")
    val cStructBitField = FqName("kotlinx.cinterop.internal.CStruct.BitField")
    val objCMethodImp = FqName("kotlinx.cinterop.ObjCMethodImp")
    val independent = FqName("kotlin.native.internal.Independent")
    val filterExceptions = FqName("kotlin.native.internal.FilterExceptions")
    val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
    val kotlinNativeInternalTestPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal", "test"))
    val kotlinxCInteropInternalPackageName = FqName.fromSegments(listOf("kotlinx", "cinterop", "internal"))
    val kotlinNativeCoroutinesInternalPackageName = FqName.fromSegments(listOf("kotlin", "coroutines", "native", "internal"))
    val associatedObjectKey = FqName("kotlin.reflect.AssociatedObjectKey")
    val typedIntrinsicAnnotation = FqName("kotlin.native.internal.TypedIntrinsic")
}
