/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

object InteropFqNames {

    const val cPointerName = "CPointer"
    const val nativePointedName = "NativePointed"

    val packageName = FqName("kotlinx.cinterop")

    val cPointer = packageName.child(Name.identifier(cPointerName)).toUnsafe()
    val nativePointed = packageName.child(Name.identifier(nativePointedName)).toUnsafe()
}

internal class InteropBuiltIns(builtIns: KonanBuiltIns, vararg konanPrimitives: ClassDescriptor) {

    val packageScope = builtIns.builtInsModule.getPackage(InteropFqNames.packageName).memberScope

    val nativePointed = packageScope.getContributedClass(InteropFqNames.nativePointedName)

    val cPointer = this.packageScope.getContributedClass(InteropFqNames.cPointerName)

    val cPointerRawValue = cPointer.unsubstitutedMemberScope.getContributedVariables("rawValue").single()

    val cPointerGetRawValue = packageScope.getContributedFunctions("getRawValue").single {
        val extensionReceiverParameter = it.extensionReceiverParameter
        extensionReceiverParameter != null &&
                TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == cPointer
    }

    val nativePointedRawPtrGetter =
            nativePointed.unsubstitutedMemberScope.getContributedVariables("rawPtr").single().getter!!

    val nativePointedGetRawPointer = packageScope.getContributedFunctions("getRawPointer").single {
        val extensionReceiverParameter = it.extensionReceiverParameter
        extensionReceiverParameter != null &&
                TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == nativePointed
    }

    val typeOf = packageScope.getContributedFunctions("typeOf").single()

    val bitsToFloat = packageScope.getContributedFunctions("bitsToFloat").single()

    val bitsToDouble = packageScope.getContributedFunctions("bitsToDouble").single()

    val staticCFunction = packageScope.getContributedFunctions("staticCFunction").toSet()

    val concurrentPackageScope = builtIns.builtInsModule.getPackage(FqName("kotlin.native.concurrent")).memberScope

    val executeFunction = concurrentPackageScope.getContributedClass("Worker")
            .unsubstitutedMemberScope.getContributedFunctions("execute").single()

    val executeImplFunction = concurrentPackageScope.getContributedFunctions("executeImpl").single()

    val signExtend = packageScope.getContributedFunctions("signExtend").single()

    val narrow = packageScope.getContributedFunctions("narrow").single()

    val convert = packageScope.getContributedFunctions("convert").toSet()

    val cFunctionPointerInvokes = packageScope.getContributedFunctions(OperatorNameConventions.INVOKE.asString())
            .filter {
                val extensionReceiverParameter = it.extensionReceiverParameter
                it.isOperator &&
                        extensionReceiverParameter != null &&
                        TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == cPointer
            }.toSet()

    private fun KonanBuiltIns.getUnsignedClass(unsignedType: UnsignedType): ClassDescriptor =
            this.builtInsModule.findClassAcrossModuleDependencies(unsignedType.classId)!!

    val invokeImpls = mapOf(
            builtIns.unit to "invokeImplUnitRet",
            builtIns.boolean to "invokeImplBooleanRet",
            builtIns.byte to "invokeImplByteRet",
            builtIns.short to "invokeImplShortRet",
            builtIns.int to "invokeImplIntRet",
            builtIns.long to "invokeImplLongRet",
            builtIns.getUnsignedClass(UnsignedType.UBYTE) to "invokeImplUByteRet",
            builtIns.getUnsignedClass(UnsignedType.USHORT) to "invokeImplUShortRet",
            builtIns.getUnsignedClass(UnsignedType.UINT) to "invokeImplUIntRet",
            builtIns.getUnsignedClass(UnsignedType.ULONG) to "invokeImplULongRet",
            builtIns.float to "invokeImplFloatRet",
            builtIns.double to "invokeImplDoubleRet",
            cPointer to "invokeImplPointerRet"
    ).mapValues { (_, name) ->
        packageScope.getContributedFunctions(name).single()
    }.toMap()

    val objCObject = packageScope.getContributedClass("ObjCObject")

    val objCObjectBase = packageScope.getContributedClass("ObjCObjectBase")

    val allocObjCObject = packageScope.getContributedFunctions("allocObjCObject").single()

    val getObjCClass = packageScope.getContributedFunctions("getObjCClass").single()

    val objCObjectRawPtr = packageScope.getContributedFunctions("objcPtr").single()

    val interpretObjCPointerOrNull = packageScope.getContributedFunctions("interpretObjCPointerOrNull").single()
    val interpretObjCPointer = packageScope.getContributedFunctions("interpretObjCPointer").single()

    val objCObjectSuperInitCheck = packageScope.getContributedFunctions("superInitCheck").single()
    val objCObjectInitBy = packageScope.getContributedFunctions("initBy").single()

    val objCAction = packageScope.getContributedClass("ObjCAction")

    val objCOutlet = packageScope.getContributedClass("ObjCOutlet")

    val objCOverrideInit = objCObjectBase.unsubstitutedMemberScope.getContributedClass("OverrideInit")

    val objCMethodImp = packageScope.getContributedClass("ObjCMethodImp")

    val exportObjCClass = packageScope.getContributedClass("ExportObjCClass")

    val CreateNSStringFromKString = packageScope.getContributedFunctions("CreateNSStringFromKString").single()

}

private fun MemberScope.getContributedVariables(name: String) =
        this.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedClass(name: String): ClassDescriptor =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS) as ClassDescriptor

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)
