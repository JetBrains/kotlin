/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

private val cPointerName = "CPointer"
private val nativePointedName = "NativePointed"

internal class InteropBuiltIns(builtIns: KonanBuiltIns) {

    object FqNames {
        val packageName = FqName("kotlinx.cinterop")

        val cPointer = packageName.child(Name.identifier(cPointerName)).toUnsafe()
        val nativePointed = packageName.child(Name.identifier(nativePointedName)).toUnsafe()
    }

    private val packageScope = builtIns.builtInsModule.getPackage(FqNames.packageName).memberScope

    val getPointerSize = packageScope.getContributedFunctions("getPointerSize").single()

    val nullableInteropValueTypes = listOf(ValueType.C_POINTER, ValueType.NATIVE_POINTED)

    private val nativePointed = packageScope.getContributedClassifier(nativePointedName) as ClassDescriptor

    val cPointer = this.packageScope.getContributedClassifier(cPointerName) as ClassDescriptor

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

    val interpretNullablePointed = packageScope.getContributedFunctions("interpretNullablePointed").single()

    val interpretCPointer = packageScope.getContributedFunctions("interpretCPointer").single()

    val typeOf = packageScope.getContributedFunctions("typeOf").single()

    val nativeMemUtils = packageScope.getContributedClassifier("nativeMemUtils") as ClassDescriptor

    private val primitives = listOf(
            builtIns.byte, builtIns.short, builtIns.int, builtIns.long,
            builtIns.float, builtIns.double,
            builtIns.nativePtr
    )

    val readPrimitive = primitives.map {
        nativeMemUtils.unsubstitutedMemberScope.getContributedFunctions("get" + it.name).single()
    }.toSet()

    val writePrimitive = primitives.map {
        nativeMemUtils.unsubstitutedMemberScope.getContributedFunctions("put" + it.name).single()
    }.toSet()

    val bitsToFloat = packageScope.getContributedFunctions("bitsToFloat").single()

    val bitsToDouble = packageScope.getContributedFunctions("bitsToDouble").single()

    val staticCFunction = packageScope.getContributedFunctions("staticCFunction").toSet()

    val workerPackageScope = builtIns.builtInsModule.getPackage(FqName("konan.worker")).memberScope

    val scheduleFunction = (workerPackageScope.getContributedClassifier("Worker") as ClassDescriptor).
            unsubstitutedMemberScope.getContributedFunctions("schedule").single()

    val scheduleImplFunction = workerPackageScope.getContributedFunctions("scheduleImpl").single()

    val signExtend = packageScope.getContributedFunctions("signExtend").single()

    val narrow = packageScope.getContributedFunctions("narrow").single()

    val readBits = packageScope.getContributedFunctions("readBits").single()
    val writeBits = packageScope.getContributedFunctions("writeBits").single()

    val cFunctionPointerInvokes = packageScope.getContributedFunctions(OperatorNameConventions.INVOKE.asString())
            .filter {
                val extensionReceiverParameter = it.extensionReceiverParameter
                it.isOperator &&
                        extensionReceiverParameter != null &&
                        TypeUtils.getClassDescriptor(extensionReceiverParameter.type) == cPointer
            }.toSet()

    val invokeImpls = mapOf(
            builtIns.unit to "invokeImplUnitRet",
            builtIns.byte to "invokeImplByteRet",
            builtIns.short to "invokeImplShortRet",
            builtIns.int to "invokeImplIntRet",
            builtIns.long to "invokeImplLongRet",
            builtIns.float to "invokeImplFloatRet",
            builtIns.double to "invokeImplDoubleRet",
            cPointer to "invokeImplPointerRet"
    ).mapValues { (_, name) ->
        packageScope.getContributedFunctions(name).single()
    }.toMap()

    val objCObject = packageScope.getContributedClassifier("ObjCObject") as ClassDescriptor
    val objCPointerHolder = packageScope.getContributedClassifier("ObjCPointerHolder") as ClassDescriptor

    val objCPointerHolderValue = objCPointerHolder.unsubstitutedMemberScope
            .getContributedDescriptors().filterIsInstance<PropertyDescriptor>().single()

    val objCObjectInitFromPtr = packageScope.getContributedFunctions("initFromPtr").single()
    val objCObjectInitFrom = packageScope.getContributedFunctions("initFrom").single()

    val allocObjCObject = packageScope.getContributedFunctions("allocObjCObject").single()

    val getObjCClass = packageScope.getContributedFunctions("getObjCClass").single()

    val objCObjectRawPtr = packageScope.getContributedVariables("rawPtr").single {
        val extensionReceiverType = it.extensionReceiverParameter?.type
        extensionReceiverType != null && !extensionReceiverType.isMarkedNullable &&
                TypeUtils.getClassDescriptor(extensionReceiverType) == objCObject
    }

    val getObjCReceiverOrSuper = packageScope.getContributedFunctions("getReceiverOrSuper").single()

    val getObjCMessenger = packageScope.getContributedFunctions("getMessenger").single()
    val getObjCMessengerLU = packageScope.getContributedFunctions("getMessengerLU").single()

}

private fun MemberScope.getContributedVariables(name: String) =
        this.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedClassifier(name: String) =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)