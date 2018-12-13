/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.kotlinNativeInternal
import org.jetbrains.kotlin.backend.konan.llvm.findMainEntryPoint
import org.jetbrains.kotlin.backend.konan.lower.TestProcessor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.config.coroutinesPackageFqName
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import kotlin.properties.Delegates

// This is what Context collects about IR.
internal class KonanIr(context: Context, irModule: IrModuleFragment): Ir<Context>(context, irModule) {

    val propertiesWithBackingFields = mutableSetOf<PropertyDescriptor>()
    val classesDelegatedBackingFields = mutableMapOf<ClassDescriptor, MutableList<PropertyDescriptor>>()

    val originalModuleIndex = ModuleIndex(irModule)

    lateinit var moduleIndexForCodegen: ModuleIndex

    override var symbols: KonanSymbols by Delegates.notNull()

    fun get(descriptor: FunctionDescriptor): IrFunction {
        return moduleIndexForCodegen.functions[descriptor]
                ?: symbols.lazySymbolTable.referenceFunction(descriptor).owner
    }

    fun get(descriptor: ClassDescriptor): IrClass {
        return moduleIndexForCodegen.classes[descriptor]
                ?: symbols.lazySymbolTable.referenceClass(descriptor)
                        .also {
                            if (!it.isBound)
                                error(descriptor)
                        }
                        .owner
    }

    fun getFromCurrentModule(descriptor: ClassDescriptor): IrClass = moduleIndexForCodegen.classes[descriptor]!!

    fun getFromCurrentModule(descriptor: FunctionDescriptor): IrFunction = moduleIndexForCodegen.functions[descriptor]!!

    fun getEnumEntryFromCurrentModule(descriptor: ClassDescriptor): IrEnumEntry =
            originalModuleIndex.enumEntries[descriptor] ?: error(descriptor)

    fun getEnumEntry(descriptor: ClassDescriptor): IrEnumEntry {
        assert(descriptor.kind == ClassKind.ENUM_ENTRY)

        return originalModuleIndex.enumEntries[descriptor]
                ?: symbols.lazySymbolTable.referenceEnumEntry(descriptor).owner
    }

    fun translateErased(type: KotlinType): IrSimpleType = symbols.symbolTable.translateErased(type)

    fun translateBroken(type: KotlinType): IrType {
        val declarationDescriptor = type.constructor.declarationDescriptor
        return when (declarationDescriptor) {
            is ClassDescriptor -> {
                val classifier = IrClassSymbolImpl(declarationDescriptor)
                val typeArguments = type.arguments.map {
                    if (it.isStarProjection) {
                        IrStarProjectionImpl
                    } else {
                        makeTypeProjection(translateBroken(it.type), it.projectionKind)
                    }
                }
                IrSimpleTypeImpl(
                        classifier,
                        type.isMarkedNullable,
                        typeArguments,
                        emptyList()
                )
            }
            is TypeParameterDescriptor -> IrSimpleTypeImpl(
                    IrTypeParameterSymbolImpl(declarationDescriptor),
                    type.isMarkedNullable,
                    emptyList(),
                    emptyList()
            )
            else -> error(declarationDescriptor ?: "null")
        }
    }
}

internal class KonanSymbols(context: Context, val symbolTable: SymbolTable, val lazySymbolTable: ReferenceSymbolTable): Symbols<Context>(context, lazySymbolTable) {
    /**
     * @note:
     * [lateinitIsInitializedPropertyGetter] is used in [org.jetbrains.kotlin.backend.common.lower.LateinitLowering] and
     * it's irrelevant for [org.jetbrains.kotlin.backend.konan.lower.LateinitLowering].
     */
    override val lateinitIsInitializedPropertyGetter: IrSimpleFunctionSymbol
       get() = TODO("unimplemented")

    val entryPoint = findMainEntryPoint(context)?.let { symbolTable.referenceSimpleFunction(it) }

    override val externalSymbolTable = lazySymbolTable

    val nothing = symbolTable.referenceClass(builtIns.nothing)
    val throwable = symbolTable.referenceClass(builtIns.throwable)
    val string = symbolTable.referenceClass(builtIns.string)
    val enum = symbolTable.referenceClass(builtIns.enum)
    val nativePtr = symbolTable.referenceClass(context.nativePtr)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())

    private fun unsignedClass(unsignedType: UnsignedType): IrClassSymbol =
            symbolTable.referenceClass(builtIns.builtInsModule.findClassAcrossModuleDependencies(unsignedType.classId)!!)

    val uByte = unsignedClass(UnsignedType.UBYTE)
    val uShort = unsignedClass(UnsignedType.USHORT)
    val uInt = unsignedClass(UnsignedType.UINT)
    val uLong = unsignedClass(UnsignedType.ULONG)

    val signedIntegerClasses = setOf(byte, short, int, long)
    val unsignedIntegerClasses = setOf(uByte, uShort, uInt, uLong)

    val allIntegerClasses = signedIntegerClasses + unsignedIntegerClasses

    val unsignedToSignedOfSameBitWidth = unsignedIntegerClasses.associate {
        it to when (it) {
            uByte -> byte
            uShort -> short
            uInt -> int
            uLong -> long
            else -> error(it.descriptor)
        }
    }

    val integerConversions = allIntegerClasses.flatMap { fromClass ->
        allIntegerClasses.map { toClass ->
            val name = Name.identifier("to${toClass.descriptor.name.asString().capitalize()}")
            val descriptor = if (fromClass in signedIntegerClasses && toClass in unsignedIntegerClasses) {
                builtInsPackage("kotlin")
                        .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                        .single {
                            it.dispatchReceiverParameter == null &&
                                    it.extensionReceiverParameter?.type == fromClass.descriptor.defaultType &&
                                    it.valueParameters.isEmpty()
                        }
            } else {
                fromClass.descriptor.unsubstitutedMemberScope
                        .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                        .single {
                            it.extensionReceiverParameter == null && it.valueParameters.isEmpty()
                        }
            }
            val symbol = symbolTable.referenceSimpleFunction(descriptor)

            (fromClass to toClass) to symbol
        }
    }.toMap()

    val arrayList = symbolTable.referenceClass(getArrayListClassDescriptor(context))

    val interopNativePointedGetRawPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedGetRawPointer)

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cPointerGetRawValue)

    val interopAllocObjCObject = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocObjCObject)

    val interopObjCRelease = symbolTable.referenceSimpleFunction(
            context.interopBuiltIns.packageScope
                    .getContributedFunctions(Name.identifier("objc_release"), NoLookupLocation.FROM_BACKEND)
                    .single()
    )

    val interopGetObjCClass = symbolTable.referenceSimpleFunction(context.interopBuiltIns.getObjCClass)

    val interopObjCObjectSuperInitCheck =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectSuperInitCheck)

    val interopObjCObjectInitBy = symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectInitBy)

    val interopObjCObjectRawValueGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectRawPtr)

    val interopInvokeImpls = context.interopBuiltIns.invokeImpls.mapValues { (_, function) ->
        symbolTable.referenceSimpleFunction(function)
    }

    val interopInterpretObjCPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointer)

    val interopInterpretObjCPointerOrNull =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointerOrNull)

    val interopCreateNSStringFromKString =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.CreateNSStringFromKString)

    val objCExportTrapOnUndeclaredException =
            symbolTable.referenceSimpleFunction(context.builtIns.kotlinNativeInternal.getContributedFunctions(
                    Name.identifier("trapOnUndeclaredException"),
                    NoLookupLocation.FROM_BACKEND
            ).single())

    val getNativeNullPtr = symbolTable.referenceSimpleFunction(context.getNativeNullPtr)

    val boxCachePredicates = BoxCache.values().associate {
        val name = "in${it.name.toLowerCase().capitalize()}BoxCache"
        it to symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())
    }

    val boxCacheGetters = BoxCache.values().associate {
        val name = "getCached${it.name.toLowerCase().capitalize()}Box"
        it to symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())
    }

    val immutableBlob = symbolTable.referenceClass(
            builtInsPackage("kotlin", "native").getContributedClassifier(
                    Name.identifier("ImmutableBlob"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val executeImpl = symbolTable.referenceSimpleFunction(context.interopBuiltIns.executeImplFunction)

    val areEqualByValue = context.getInternalFunctions("areEqualByValue").map {
        symbolTable.referenceSimpleFunction(it)
    }.associateBy { it.descriptor.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()!! }

    val reinterpret = symbolTable.referenceSimpleFunction(context.getInternalFunctions("reinterpret").single())

    val ieee754Equals = context.getInternalFunctions("ieee754Equals").map {
        symbolTable.referenceSimpleFunction(it)
    }

    val equals = context.builtIns.any.unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier("equals"), NoLookupLocation.FROM_BACKEND)
            .single().let { symbolTable.referenceSimpleFunction(it) }

    override val areEqual get() = error("Must not be used")

    val throwArithmeticException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowArithmeticException").single())

    override val ThrowNullPointerException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowNullPointerException").single())

    override val ThrowNoWhenBranchMatchedException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowNoWhenBranchMatchedException").single())

    override val ThrowTypeCastException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowTypeCastException").single())

    val throwInvalidReceiverTypeException = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("ThrowInvalidReceiverTypeException").single())

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

    val arrayContentToString = arrays.associateBy(
            { it },
            { findArrayExtension(it.descriptor, "contentToString") }
    )
    val arrayContentHashCode = arrays.associateBy(
            { it },
            { findArrayExtension(it.descriptor, "contentHashCode") }
    )

    private fun findArrayExtension(descriptor: ClassDescriptor, name: String): IrSimpleFunctionSymbol {
        val functionDescriptor = builtInsPackage("kotlin", "collections")
                .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                .singleOrNull {
                    it.valueParameters.isEmpty()
                            && it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == descriptor
                            && !it.isExpect
                } ?: error(descriptor.toString())
        return symbolTable.referenceSimpleFunction(functionDescriptor)
    }
    override val copyRangeTo = arrays.map { symbol ->
        val packageViewDescriptor = builtIns.builtInsModule.getPackage(KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME)
        val functionDescriptor = packageViewDescriptor.memberScope
                .getContributedFunctions(Name.identifier("copyRangeTo"), NoLookupLocation.FROM_BACKEND)
                .first {
                    it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == symbol.descriptor
                }
        symbol.descriptor to symbolTable.referenceSimpleFunction(functionDescriptor)
    }.toMap()

    val arrayGet = arrays.associateWith { it.descriptor.unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier("get"), NoLookupLocation.FROM_BACKEND)
            .single().let { symbolTable.referenceSimpleFunction(it) } }

    val arraySet = arrays.associateWith { it.descriptor.unsubstitutedMemberScope
                    .getContributedFunctions(Name.identifier("set"), NoLookupLocation.FROM_BACKEND)
                    .single().let { symbolTable.referenceSimpleFunction(it) } }


    val arraySize = arrays.associateWith { it.descriptor.unsubstitutedMemberScope
                    .getContributedVariables(Name.identifier("size"), NoLookupLocation.FROM_BACKEND)
                    .single().let { symbolTable.referenceSimpleFunction(it.getter!!) } }


    val valuesForEnum = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("valuesForEnum").single())

    val valueOfForEnum = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("valueOfForEnum").single())

    val enumValues = symbolTable.referenceSimpleFunction(
             builtInsPackage("kotlin").getContributedFunctions(
                     Name.identifier("enumValues"), NoLookupLocation.FROM_BACKEND).single())

    val enumValueOf = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin").getContributedFunctions(
                    Name.identifier("enumValueOf"), NoLookupLocation.FROM_BACKEND).single())

    val createUninitializedInstance = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("createUninitializedInstance").single())

    val initInstance = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("initInstance").single())

    val freeze = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin", "native", "concurrent").getContributedFunctions(
                    Name.identifier("freeze"), NoLookupLocation.FROM_BACKEND).single())

    val println = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin", "io").getContributedFunctions(
                    Name.identifier("println"), NoLookupLocation.FROM_BACKEND)
                    .single { it.valueParameters.singleOrNull()?.type == builtIns.stringType })

    val anyNToString = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin").getContributedFunctions(
                    Name.identifier("toString"), NoLookupLocation.FROM_BACKEND)
                    .single { it.extensionReceiverParameter?.type == builtIns.nullableAnyType})

    val getContinuation = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("getContinuation").single())

    val konanSuspendCoroutineUninterceptedOrReturn = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("suspendCoroutineUninterceptedOrReturn").single())

    val konanCoroutineContextGetter = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("getCoroutineContext").single())

    private val coroutinesIntrinsicsPackage = context.builtIns.builtInsModule.getPackage(
        context.config.configuration.languageVersionSettings.coroutinesIntrinsicsPackageFqName()).memberScope

    private val coroutinesPackage = context.builtIns.builtInsModule.getPackage(
            context.config.configuration.languageVersionSettings.coroutinesPackageFqName()).memberScope

    val continuationClassDescriptor = coroutinesPackage
            .getContributedClassifier(Name.identifier("Continuation"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    val coroutineContextGetter = coroutinesPackage
            .getContributedVariables(Name.identifier("coroutineContext"), NoLookupLocation.FROM_BACKEND)
            .single()
            .getter!!

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = symbolTable.referenceClass(
            builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.coroutines.native.internal.BaseContinuationImpl")))!!
    )

    val restrictedContinuationImpl = symbolTable.referenceClass(
            builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.coroutines.native.internal.RestrictedContinuationImpl")))!!
    )

    val continuationImpl = symbolTable.referenceClass(
            builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.coroutines.native.internal.ContinuationImpl")))!!
    )

    override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            coroutinesIntrinsicsPackage
                    .getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND)
                    .filterNot { it.isExpect }.single().getter!!
    )

    val kotlinResult = symbolTable.referenceClass(
            builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.Result")))!!
    )

    val kotlinResultGetOrThrow = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin")
                    .getContributedFunctions(Name.identifier("getOrThrow"), NoLookupLocation.FROM_BACKEND)
                    .single {
                        it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == kotlinResult.descriptor
                    }
    )

    val refClass = symbolTable.referenceClass(context.getInternalClass("Ref"))

    val isInitializedPropertyDescriptor = builtInsPackage("kotlin")
            .getContributedVariables(Name.identifier("isInitialized"), NoLookupLocation.FROM_BACKEND).single {
                it.extensionReceiverParameter.let {
                    it != null && TypeUtils.getClassDescriptor(it.type) == context.reflectionTypes.kProperty0
                } && !it.isExpect
            }

    val isInitializedGetterDescriptor = isInitializedPropertyDescriptor.getter!!

    val kFunctionImpl =  symbolTable.referenceClass(context.reflectionTypes.kFunctionImpl)

    val kProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty0Impl)
    val kProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty1Impl)
    val kProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kProperty2Impl)
    val kMutableProperty0Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty0Impl)
    val kMutableProperty1Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty1Impl)
    val kMutableProperty2Impl = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty2Impl)

    val kLocalDelegatedPropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedPropertyImpl)
    val kLocalDelegatedMutablePropertyImpl = symbolTable.referenceClass(context.reflectionTypes.kLocalDelegatedMutablePropertyImpl)

    val getClassTypeInfo = internalFunction("getClassTypeInfo")
    val getObjectTypeInfo = internalFunction("getObjectTypeInfo")
    val kClassImpl = internalClass("KClassImpl")
    val kClassImplConstructor by lazy { kClassImpl.constructors.single() }
    val kClassUnsupportedImpl = internalClass("KClassUnsupportedImpl")
    val kClassUnsupportedImplConstructor by lazy { kClassUnsupportedImpl.constructors.single() }
    val kTypeImpl = internalClass("KTypeImpl")
    val kTypeImplForGenerics = internalClass("KTypeImplForGenerics")

    val listOfInternal = internalFunction("listOfInternal")

    val threadLocal =
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.native.ThreadLocal")))!!

    val sharedImmutable =
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.native.SharedImmutable")))!!

    private fun internalFunction(name: String): IrSimpleFunctionSymbol =
            symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())

    private fun internalClass(name: String): IrClassSymbol =
            symbolTable.referenceClass(context.getInternalClass(name))

    private fun getKonanTestClass(className: String) = symbolTable.referenceClass(
            builtInsPackage("kotlin", "native", "internal", "test").getContributedClassifier(
                    Name.identifier(className), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor)

    private fun getFunction(name: Name, receiverType: KotlinType, predicate: (FunctionDescriptor) -> Boolean) =
            symbolTable.referenceFunction(receiverType.memberScope
                    .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).single(predicate)
            )

    val functions = (0 .. KONAN_FUNCTION_INTERFACES_MAX_PARAMETERS)
            .map { symbolTable.referenceClass(builtIns.getFunction(it)) }

    val kFunctions = (0 .. KONAN_FUNCTION_INTERFACES_MAX_PARAMETERS)
            .map { symbolTable.referenceClass(context.reflectionTypes.getKFunction(it)) }

    fun getKFunctionType(returnType: IrType, parameterTypes: List<IrType>): IrType {
        val kFunctionClassSymbol = kFunctions[parameterTypes.size]
        return kFunctionClassSymbol.typeWith(parameterTypes + returnType)
    }

    val suspendFunctions = (0 .. KONAN_FUNCTION_INTERFACES_MAX_PARAMETERS)
            .map { symbolTable.referenceClass(builtIns.getSuspendFunction(it)) }

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
        symbolTable.referenceEnumEntry(testFunctionKind.descriptor.unsubstitutedMemberScope.getContributedClassifier(
                kind.runtimeKindName, NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor)
    }
}

private fun getArrayListClassDescriptor(context: Context): ClassDescriptor {
    val module = context.builtIns.builtInsModule
    val pkg = module.getPackage(FqName.fromSegments(listOf("kotlin", "collections")))
    val classifier = pkg.memberScope.getContributedClassifier(Name.identifier("ArrayList"),
            NoLookupLocation.FROM_BACKEND)

    return classifier as ClassDescriptor
}
