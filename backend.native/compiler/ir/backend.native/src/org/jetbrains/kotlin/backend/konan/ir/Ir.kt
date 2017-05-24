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

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.konanInternal
import org.jetbrains.kotlin.backend.konan.util.atMostOne
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.SymbolTable
import org.jetbrains.kotlin.util.OperatorNameConventions

// This is what Context collects about IR.
internal class Ir(val context: Context, val irModule: IrModuleFragment) {
    val propertiesWithBackingFields = mutableSetOf<PropertyDescriptor>()

    val defaultParameterDeclarationsCache = mutableMapOf<FunctionDescriptor, IrFunction>()

    val originalModuleIndex = ModuleIndex(irModule)

    lateinit var moduleIndexForCodegen: ModuleIndex

    lateinit var symbols: Symbols
}

internal class Symbols(val context: Context, val symbolTable: SymbolTable) {

    val builtIns: KonanBuiltIns get() = context.builtIns

    private fun builtInsPackage(vararg packageNameSegments: String) =
            builtIns.builtInsModule.getPackage(FqName.fromSegments(listOf(*packageNameSegments))).memberScope

    val refClass = symbolTable.referenceClass(builtIns.getKonanInternalClass("Ref"))

    val areEqualByValue = builtIns.getKonanInternalFunctions("areEqualByValue").map {
        symbolTable.referenceSimpleFunction(it)
    }

    val areEqual = symbolTable.referenceSimpleFunction(builtIns.getKonanInternalFunctions("areEqual").single())

    val ThrowNullPointerException = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("ThrowNullPointerException").single())

    val ThrowNoWhenBranchMatchedException = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("ThrowNoWhenBranchMatchedException").single())

    val ThrowTypeCastException = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("ThrowTypeCastException").single())

    val ThrowUninitializedPropertyAccessException = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("ThrowUninitializedPropertyAccessException").single()
    )

    val stringBuilder = symbolTable.referenceClass(
            builtInsPackage("kotlin", "text").getContributedClassifier(
                    Name.identifier("StringBuilder"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val defaultArgumentMarker = symbolTable.referenceClass(
            builtIns.konanInternal.getContributedClassifier(
                    Name.identifier("DefaultArgumentMarker"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val any = symbolTable.referenceClass(builtIns.any)
    val unit = symbolTable.referenceClass(builtIns.unit)

    val byte = symbolTable.referenceClass(builtIns.byte)
    val short = symbolTable.referenceClass(builtIns.short)
    val int = symbolTable.referenceClass(builtIns.int)
    val long = symbolTable.referenceClass(builtIns.long)

    val integerClasses = listOf(byte, short, int, long)

    val arrayOf = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin").getContributedFunctions(
                    Name.identifier("arrayOf"), NoLookupLocation.FROM_BACKEND
            ).single()
    )

    val array = symbolTable.referenceClass(builtIns.array)

    private fun primitiveArrayClass(type: PrimitiveType) =
            symbolTable.referenceClass(builtIns.getPrimitiveArrayClassDescriptor(type))

    val byteArray = primitiveArrayClass(PrimitiveType.BYTE)
    val charArray = primitiveArrayClass(PrimitiveType.CHAR)
    val shortArray = primitiveArrayClass(PrimitiveType.SHORT)
    val intArray = primitiveArrayClass(PrimitiveType.INT)
    val longArray = primitiveArrayClass(PrimitiveType.LONG)
    val floatArray = primitiveArrayClass(PrimitiveType.FLOAT)
    val doubleArray = primitiveArrayClass(PrimitiveType.DOUBLE)
    val booleanArray = primitiveArrayClass(PrimitiveType.BOOLEAN)

    val arrays = PrimitiveType.values().map { primitiveArrayClass(it) } + array

    val copyRangeTo = arrays.map { symbol ->
        val packageViewDescriptor = builtIns.builtInsModule.getPackage(KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME)
        val functionDescriptor = packageViewDescriptor.memberScope
                .getContributedFunctions(Name.identifier("copyRangeTo"), NoLookupLocation.FROM_BACKEND)
                .first {
                    it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == symbol.descriptor
                }
        symbol.descriptor to symbolTable.referenceSimpleFunction(functionDescriptor)
    }.toMap()

    val intAnd = symbolTable.referenceFunction(
            builtIns.intType.memberScope
                    .getContributedFunctions(OperatorNameConventions.AND, NoLookupLocation.FROM_BACKEND)
                    .single()
    )

    val intPlusInt = symbolTable.referenceFunction(
            builtIns.intType.memberScope
                    .getContributedFunctions(OperatorNameConventions.PLUS, NoLookupLocation.FROM_BACKEND)
                    .single {
                        it.valueParameters.single().type == builtIns.intType
                    }
    )

    val boxFunctions = ValueType.values().associate {
        val boxFunctionName = "box${it.classFqName.shortName()}"
        it to symbolTable.referenceSimpleFunction(context.builtIns.getKonanInternalFunctions(boxFunctionName).single())
    }

    val boxClasses = ValueType.values().associate {
        it to symbolTable.referenceClass(context.builtIns.getKonanInternalClass("${it.classFqName.shortName()}Box"))
    }

    val unboxFunctions = ValueType.values().mapNotNull {
        val unboxFunctionName = "unbox${it.classFqName.shortName()}"
        context.builtIns.getKonanInternalFunctions(unboxFunctionName).atMostOne()?.let { descriptor ->
            it to symbolTable.referenceSimpleFunction(descriptor)
        }
    }.toMap()

    val interopNativePointedGetRawPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedGetRawPointer)

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cPointerGetRawValue)

    val getNativeNullPtr = symbolTable.referenceSimpleFunction(builtIns.getNativeNullPtr)

    val valuesForEnum = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("valuesForEnum").single())

    val valueOfForEnum = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("valueOfForEnum").single())

    val getContinuation = symbolTable.referenceSimpleFunction(
            context.builtIns.getKonanInternalFunctions("getContinuation").single())

    val coroutineImpl = symbolTable.referenceClass(context.builtIns.getKonanInternalClass("CoroutineImpl"))

    val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin", "coroutines", "experimental", "intrinsics")
                    .getContributedVariables(Name.identifier("COROUTINE_SUSPENDED"), NoLookupLocation.FROM_BACKEND)
                    .single().getter!!
    )

    val kFunctionImpl = symbolTable.referenceClass(context.builtIns.getKonanInternalClass("KFunctionImpl"))

    val kProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty0Impl)
    val kProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty1Impl)
    val kProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty2Impl)
    val kMutableProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty0Impl)
    val kMutableProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty1Impl)
    val kMutableProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty2Impl)
    val kLocalDelegatedPropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedPropertyImpl)
    val kLocalDelegatedMutablePropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedMutablePropertyImpl)

}