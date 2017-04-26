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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

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

    val memberAt = packageScope.getContributedFunctions("memberAt").single()

    val interpretNullablePointed = packageScope.getContributedFunctions("interpretNullablePointed").single()

    val interpretCPointer = packageScope.getContributedFunctions("interpretCPointer").single()

    val variableClass = packageScope.getContributedClassifier("CVariable") as ClassDescriptor

    val arrayGetByIntIndex = packageScope.getContributedFunctions("get").single {
        KotlinBuiltIns.isInt(it.valueParameters.single().type) &&
                it.typeParameters.single().upperBounds.single() == variableClass.defaultType
    }

    val arrayGetByLongIndex = packageScope.getContributedFunctions("get").single {
        KotlinBuiltIns.isLong(it.valueParameters.single().type) &&
                it.typeParameters.single().upperBounds.single() == variableClass.defaultType
    }

    val allocUninitializedArrayWithIntLength = packageScope.getContributedFunctions("allocArray").single {
        it.valueParameters.size == 1 && KotlinBuiltIns.isInt(it.valueParameters[0].type)
    }

    val allocUninitializedArrayWithLongLength = packageScope.getContributedFunctions("allocArray").single {
        it.valueParameters.size == 1 && KotlinBuiltIns.isLong(it.valueParameters[0].type)
    }

    val allocVariable = packageScope.getContributedFunctions("alloc").single {
        it.valueParameters.size == 0
    }

    val readValue = packageScope.getContributedFunctions("readValue").single {
        it.valueParameters.size == 0
    }

    val readValueBySizeAndAlign = packageScope.getContributedFunctions("readValue").single {
        it.valueParameters.size == 2
    }

    val typeOf = packageScope.getContributedFunctions("typeOf").single()

    val variableTypeClass =
            variableClass.unsubstitutedInnerClassesScope.getContributedClassifier("Type") as ClassDescriptor

    val variableTypeSize = variableTypeClass.unsubstitutedMemberScope.getContributedVariables("size").single()

    val variableTypeAlign = variableTypeClass.unsubstitutedMemberScope.getContributedVariables("align").single()

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

    val signExtend = packageScope.getContributedFunctions("signExtend").single()

    val narrow = packageScope.getContributedFunctions("narrow").single()
}

private fun MemberScope.getContributedVariables(name: String) =
        this.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedClassifier(name: String) =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)