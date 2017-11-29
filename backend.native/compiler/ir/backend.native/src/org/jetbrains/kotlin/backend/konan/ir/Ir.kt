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

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.descriptors.getMemberScope
import org.jetbrains.kotlin.backend.konan.lower.TestProcessor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import kotlin.properties.Delegates

// This is what Context collects about IR.
internal class KonanIr(context: Context, irModule: IrModuleFragment): Ir<Context>(context, irModule) {

    val propertiesWithBackingFields = mutableSetOf<PropertyDescriptor>()

    val originalModuleIndex = ModuleIndex(irModule)

    lateinit var moduleIndexForCodegen: ModuleIndex

    override var symbols: KonanSymbols by Delegates.notNull()
}

internal class KonanSymbols(context: Context, val symbolTable: SymbolTable): Symbols<Context>(context, symbolTable) {

    val interopNativePointedGetRawPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedGetRawPointer)

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cPointerGetRawValue)

    val interopAllocObjCObject = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocObjCObject)

    val interopGetObjCClass = symbolTable.referenceSimpleFunction(context.interopBuiltIns.getObjCClass)

    val interopObjCObjectInitFromPtr =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectInitFromPtr)

    val interopObjCObjectSuperInitCheck =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectSuperInitCheck)

    val interopObjCObjectRawValueGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectRawPtr.getter!!)

    val interopInvokeImpls = context.interopBuiltIns.invokeImpls.mapValues { (_, function) ->
        symbolTable.referenceSimpleFunction(function)
    }

    val interopInterpretObjCPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointer)

    val interopInterpretObjCPointerOrNull =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointerOrNull)

    val getNativeNullPtr = symbolTable.referenceSimpleFunction(context.builtIns.getNativeNullPtr)

    val boxFunctions = ValueType.values().associate {
        val boxFunctionName = "box${it.classFqName.shortName()}"
        it to symbolTable.referenceSimpleFunction(context.getInternalFunctions(boxFunctionName).single())
    }

    val boxClasses = ValueType.values().associate {
        it to symbolTable.referenceClass(context.getInternalClass("${it.classFqName.shortName()}Box"))
    }

    val valueClassToBox = ValueType.values().associate {
        val valueClassId = ClassId.topLevel(it.classFqName.toSafe())
        val valueClassDescriptor = context.builtIns.builtInsModule.findClassAcrossModuleDependencies(valueClassId)!!
        valueClassDescriptor to boxClasses[it]!!
    }

    val unboxFunctions = ValueType.values().mapNotNull {
        val unboxFunctionName = "unbox${it.classFqName.shortName()}"
        context.getInternalFunctions(unboxFunctionName).atMostOne()?.let { descriptor ->
            it to symbolTable.referenceSimpleFunction(descriptor)
        }
    }.toMap()

    val immutableBinaryBlob = symbolTable.referenceClass(
            builtInsPackage("konan").getContributedClassifier(
                    Name.identifier("ImmutableBinaryBlob"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val immutableBinaryBlobOf = symbolTable.referenceSimpleFunction(context.builtIns.immutableBinaryBlobOf)

    val scheduleImpl = symbolTable.referenceSimpleFunction(context.interopBuiltIns.scheduleImplFunction)

    val areEqualByValue = context.getInternalFunctions("areEqualByValue").map {
        symbolTable.referenceSimpleFunction(it)
    }

    override val areEqual = symbolTable.referenceSimpleFunction(context.getInternalFunctions("areEqual").single())

    override val ThrowNullPointerException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowNullPointerException").single())

    override val ThrowNoWhenBranchMatchedException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowNoWhenBranchMatchedException").single())

    override val ThrowTypeCastException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowTypeCastException").single())

    override val ThrowUninitializedPropertyAccessException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowUninitializedPropertyAccessException").single()
    )

    override val stringBuilder = symbolTable.referenceClass(
            builtInsPackage("kotlin", "text").getContributedClassifier(
                    Name.identifier("StringBuilder"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val checkProgressionStep = context.getInternalFunctions("checkProgressionStep")
            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()
    val getProgressionLast = context.getInternalFunctions("getProgressionLast")
            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()

    val arrayContentToString = arrayTypes.associateBy({ it }, { arrayExtensionFun(it, "contentToString") })
    val arrayContentHashCode = arrayTypes.associateBy({ it }, { arrayExtensionFun(it, "contentHashCode") })

    override val copyRangeTo = arrays.map { symbol ->
        val packageViewDescriptor = builtIns.builtInsModule.getPackage(KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME)
        val functionDescriptor = packageViewDescriptor.memberScope
                .getContributedFunctions(Name.identifier("copyRangeTo"), NoLookupLocation.FROM_BACKEND)
                .first {
                    it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == symbol.descriptor
                }
        symbol.descriptor to symbolTable.referenceSimpleFunction(functionDescriptor)
    }.toMap()

    val valuesForEnum = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("valuesForEnum").single())

    val valueOfForEnum = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("valueOfForEnum").single())

    val enumValues = symbolTable.referenceSimpleFunction(
             builtInsPackage("kotlin").getContributedFunctions(Name.identifier("enumValues"), NoLookupLocation.FROM_BACKEND).single())

    val enumValueOf = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin").getContributedFunctions(Name.identifier("enumValueOf"), NoLookupLocation.FROM_BACKEND).single())

    val createUninitializedInstance = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("createUninitializedInstance").single())

    val initInstance = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("initInstance").single())

    val getContinuation = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("getContinuation").single())

    override val coroutineImpl = symbolTable.referenceClass(context.getInternalClass("CoroutineImpl"))

    override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin", "coroutines", "experimental", "intrinsics")
                    .getContributedVariables(Name.identifier("COROUTINE_SUSPENDED"), NoLookupLocation.FROM_BACKEND)
                    .single().getter!!
    )


    val kLocalDelegatedPropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedPropertyImpl)
    val kLocalDelegatedMutablePropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedMutablePropertyImpl)

    val getClassTypeInfo = internalFunction("getClassTypeInfo")
    val getObjectTypeInfo = internalFunction("getObjectTypeInfo")
    val kClassImpl = internalClass("KClassImpl")
    val kClassImplConstructor by lazy { kClassImpl.constructors.single() }

    private fun internalFunction(name: String): IrSimpleFunctionSymbol =
            symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())

    private fun internalClass(name: String): IrClassSymbol =
            symbolTable.referenceClass(context.getInternalClass(name))

    private fun getKonanTestClass(className: String) = symbolTable.referenceClass(
            builtInsPackage("konan", "test").getContributedClassifier(
                    Name.identifier(className), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor)

    private fun getFunction(name: Name, receiverType: KotlinType, predicate: (FunctionDescriptor) -> Boolean) =
            symbolTable.referenceFunction(receiverType.memberScope
                    .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).single(predicate)
            )

    val baseClassSuite   = getKonanTestClass("BaseClassSuite")
    val topLevelSuite    = getKonanTestClass("TopLevelSuite")
    val testFunctionKind = getKonanTestClass("TestFunctionKind")

    val baseClassSuiteConstructor = baseClassSuite.descriptor.constructors.single {
        it.valueParameters.size == 2 &&
        KotlinBuiltIns.isString(it.valueParameters[0].type) && // name: String
        KotlinBuiltIns.isBoolean(it.valueParameters[1].type)   // ignored: Boolean
    }

    val topLevelSuiteConstructor = symbolTable.referenceConstructor(topLevelSuite.descriptor.constructors.single {
        it.valueParameters.size == 1 &&
        KotlinBuiltIns.isString(it.valueParameters[0].type) // name: String
    })

    val topLevelSuiteRegisterFunction =
            getFunction(Name.identifier("registerFunction"), topLevelSuite.descriptor.defaultType) {
                it.valueParameters.size == 2 &&
                it.valueParameters[0].type == testFunctionKind.descriptor.defaultType && // kind: TestFunctionKind
                it.valueParameters[1].type.isFunctionType                                // function: () -> Unit
            }

    val topLevelSuiteRegisterTestCase =
            getFunction(Name.identifier("registerTestCase"), topLevelSuite.descriptor.defaultType) {
                it.valueParameters.size == 3 &&
                KotlinBuiltIns.isString(it.valueParameters[0].type) &&  // name: String
                it.valueParameters[1].type.isFunctionType &&            // function: () -> Unit
                KotlinBuiltIns.isBoolean(it.valueParameters[2].type)    // ignored: Boolean
            }

    private val testFunctionKindCache = mutableMapOf<TestProcessor.FunctionKind, IrEnumEntrySymbol>()
    fun getTestFunctionKind(kind: TestProcessor.FunctionKind): IrEnumEntrySymbol = testFunctionKindCache.getOrPut(kind) {
        symbolTable.referenceEnumEntry(testFunctionKind.descriptor.getMemberScope().getContributedClassifier(
                kind.runtimeKindName, NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor)
    }
}
