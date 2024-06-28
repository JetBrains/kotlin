/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object NativeRuntimeNames {
    private val kotlinNativePackage = FqName("kotlin.native")
    private val kotlinNativeInternalPackage = kotlinNativePackage.child(Name.identifier("internal"))
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
    }
}