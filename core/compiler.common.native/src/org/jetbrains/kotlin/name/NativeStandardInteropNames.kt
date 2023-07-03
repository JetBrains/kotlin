/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object NativeStandardInteropNames {
    val cInteropPackage = FqName("kotlinx.cinterop")

    internal val COpaque = Name.identifier("COpaque")
    internal val CStructVar = Name.identifier("CStructVar")
    internal val ObjCObjectBase = Name.identifier("ObjCObjectBase")
    internal val ObjCObject = Name.identifier("ObjCObject")

    object ForwardDeclarations {
        private val cNamesPackage = FqName("cnames")
        val cNamesStructsPackage = cNamesPackage.child(Name.identifier("structs"))

        private val objCNamesPackage = FqName("objcnames")
        val objCNamesClassesPackage = objCNamesPackage.child(Name.identifier("classes"))
        val objCNamesProtocolsPackage = objCNamesPackage.child(Name.identifier("protocols"))

        val syntheticPackages = setOf(cNamesPackage, objCNamesPackage)
    }
}