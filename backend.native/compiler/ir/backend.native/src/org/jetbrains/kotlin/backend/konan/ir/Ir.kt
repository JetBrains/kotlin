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
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.SymbolTable

// This is what Context collects about IR.
internal class Ir(val context: Context, val irModule: IrModuleFragment) {
    val propertiesWithBackingFields = mutableSetOf<PropertyDescriptor>()

    val defaultParameterDescriptorsCache = mutableMapOf<FunctionDescriptor, FunctionDescriptor>()

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

    val stringBuilder = symbolTable.referenceClass(
            builtInsPackage("kotlin", "text").getContributedClassifier(
                    Name.identifier("StringBuilder"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val any = symbolTable.referenceClass(builtIns.any)

    val arrayOf = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin").getContributedFunctions(
                    Name.identifier("arrayOf"), NoLookupLocation.FROM_BACKEND
            ).single()
    )

    val array = symbolTable.referenceClass(builtIns.array)

    val valuesForEnum = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("valuesForEnum").single())

    val valueOfForEnum = symbolTable.referenceSimpleFunction(
            builtIns.getKonanInternalFunctions("valueOfForEnum").single())

    val kProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty0Impl)
    val kProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty1Impl)
    val kProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty2Impl)
    val kMutableProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty0Impl)
    val kMutableProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty1Impl)
    val kMutableProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty2Impl)
    val kLocalDelegatedPropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedPropertyImpl)
    val kLocalDelegatedMutablePropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedMutablePropertyImpl)

}