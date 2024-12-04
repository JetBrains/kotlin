/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

@Suppress("ConstPropertyName")
object NativeStandardInteropNames {
    val cInteropPackage = FqName("kotlinx.cinterop")

    internal val COpaque = Name.identifier("COpaque")
    internal val CStructVar = Name.identifier("CStructVar")
    internal val ObjCObjectBase = Name.identifier("ObjCObjectBase")
    internal val ObjCObject = Name.identifier("ObjCObject")
    val ExperimentalForeignApi = Name.identifier("ExperimentalForeignApi")

    val objCDirectClassId = ClassId(cInteropPackage, Name.identifier("ObjCDirect"))
    val objCMethodClassId = ClassId(cInteropPackage, Name.identifier("ObjCMethod"))
    val objCObjectClassId = ClassId(cInteropPackage, Name.identifier("ObjCObject"))
    val objCFactoryClassId = ClassId(cInteropPackage, Name.identifier("ObjCFactory"))
    val objCConstructorClassId = ClassId(cInteropPackage, Name.identifier("ObjCConstructor"))
    val externalObjCClassClassId = ClassId(cInteropPackage, Name.identifier("ExternalObjCClass"))
    val objCActionClassId = ClassId(cInteropPackage, Name.identifier("ObjCAction"))
    val objCOutletClassId = ClassId(cInteropPackage, Name.identifier("ObjCOutlet"))
    val objCOverrideInitClassId = ClassId(cInteropPackage, Name.identifier("ObjCObjectBase.OverrideInit"))

    object Annotations {
        val objCSignatureOverrideClassId = ClassId(cInteropPackage, Name.identifier("ObjCSignatureOverride"))
    }

    object ForwardDeclarations {
        private val cNamesPackage = FqName("cnames")
        val cNamesStructsPackage = cNamesPackage.child(Name.identifier("structs"))

        private val objCNamesPackage = FqName("objcnames")
        val objCNamesClassesPackage = objCNamesPackage.child(Name.identifier("classes"))
        val objCNamesProtocolsPackage = objCNamesPackage.child(Name.identifier("protocols"))

        val syntheticPackages = setOf(cNamesPackage, objCNamesPackage)
    }

    const val cTypeDefinitionsFileName = "CTypeDefinitions"
}