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
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.config.coroutinesPackageFqName
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.Delegates

// This is what Context collects about IR.
internal class KonanIr(context: Context, irModule: IrModuleFragment): Ir<Context>(context, irModule) {
    override var symbols: KonanSymbols by Delegates.notNull()
}

internal class KonanSymbols(
        context: Context,
        irBuiltIns: IrBuiltIns,
        private val symbolTable: SymbolTable,
        lazySymbolTable: ReferenceSymbolTable,
        val functionIrClassFactory: BuiltInFictitiousFunctionIrClassFactory
): Symbols<Context>(context, irBuiltIns, symbolTable) {

    val entryPoint = findMainEntryPoint(context)?.let { symbolTable.referenceSimpleFunction(it) }

    override val externalSymbolTable = lazySymbolTable

    val nothing = symbolTable.referenceClass(builtIns.nothing)
    val throwable = symbolTable.referenceClass(builtIns.throwable)
    val enum = symbolTable.referenceClass(builtIns.enum)
    val nativePtr = symbolTable.referenceClass(context.nativePtr)
    val nativePointed = symbolTable.referenceClass(context.interopBuiltIns.nativePointed)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())
    val nonNullNativePtr = symbolTable.referenceClass(context.nonNullNativePtr)

    val immutableBlobOf = symbolTable.referenceSimpleFunction(context.immutableBlobOf)

    private fun unsignedClass(unsignedType: UnsignedType): IrClassSymbol = classById(unsignedType.classId)

    override val uByte = unsignedClass(UnsignedType.UBYTE)
    override val uShort = unsignedClass(UnsignedType.USHORT)
    override val uInt = unsignedClass(UnsignedType.UINT)
    override val uLong = unsignedClass(UnsignedType.ULONG)

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

    val symbolName = topLevelClass(RuntimeNames.symbolNameAnnotation)
    val filterExceptions = topLevelClass(RuntimeNames.filterExceptions)
    val exportForCppRuntime = topLevelClass(RuntimeNames.exportForCppRuntime)

    val objCMethodImp = symbolTable.referenceClass(context.interopBuiltIns.objCMethodImp)

    val onUnhandledException = internalFunction("OnUnhandledException")

    val interopNativePointedGetRawPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedGetRawPointer)

    val interopCPointer = symbolTable.referenceClass(context.interopBuiltIns.cPointer)
    val interopCstr = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cstr.getter!!)
    val interopWcstr = symbolTable.referenceSimpleFunction(context.interopBuiltIns.wcstr.getter!!)
    val interopMemScope = symbolTable.referenceClass(context.interopBuiltIns.memScope)
    val interopCValue = symbolTable.referenceClass(context.interopBuiltIns.cValue)
    val interopCValues = symbolTable.referenceClass(context.interopBuiltIns.cValues)
    val interopCValuesRef = symbolTable.referenceClass(context.interopBuiltIns.cValuesRef)
    val interopCValueWrite = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cValueWrite)
    val interopCValueRead = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cValueRead)
    val interopAllocType = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocType)

    val interopTypeOf = symbolTable.referenceSimpleFunction(context.interopBuiltIns.typeOf)

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cPointerGetRawValue)

    val interopAllocObjCObject = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocObjCObject)

    val interopForeignObjCObject = interopClass("ForeignObjCObject")

    // These are possible supertypes of forward declarations - we need to reference them explicitly to force their deserialization.
    // TODO: Do it lazily.
    val interopCOpaque = symbolTable.referenceClass(context.interopBuiltIns.cOpaque)
    val interopObjCObject = symbolTable.referenceClass(context.interopBuiltIns.objCObject)
    val interopObjCObjectBase = symbolTable.referenceClass(context.interopBuiltIns.objCObjectBase)

    val interopObjCRelease = interopFunction("objc_release")

    val interopObjCRetain = interopFunction("objc_retain")

    val interopObjcRetainAutoreleaseReturnValue = interopFunction("objc_retainAutoreleaseReturnValue")

    val interopCreateObjCObjectHolder = interopFunction("createObjCObjectHolder")

    val interopCreateKotlinObjectHolder = interopFunction("createKotlinObjectHolder")
    val interopUnwrapKotlinObjectHolderImpl = interopFunction("unwrapKotlinObjectHolderImpl")

    val interopCreateObjCSuperStruct = interopFunction("createObjCSuperStruct")

    val interopGetMessenger = interopFunction("getMessenger")
    val interopGetMessengerStret = interopFunction("getMessengerStret")

    val interopGetObjCClass = symbolTable.referenceSimpleFunction(context.interopBuiltIns.getObjCClass)

    val interopObjCObjectSuperInitCheck =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectSuperInitCheck)

    val interopObjCObjectInitBy = symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectInitBy)

    val interopObjCObjectRawValueGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectRawPtr)

    val interopNativePointedRawPtrGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.nativePointedRawPtrGetter)

    val interopCPointerRawValue =
            symbolTable.referenceProperty(context.interopBuiltIns.cPointerRawValue)

    val interopInterpretObjCPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointer)

    val interopInterpretObjCPointerOrNull =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretObjCPointerOrNull)

    val interopInterpretNullablePointed =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretNullablePointed)

    val interopInterpretCPointer =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.interpretCPointer)

    val interopCreateNSStringFromKString =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.CreateNSStringFromKString)

    val interopObjCGetSelector = interopFunction("objCGetSelector")

    val interopCEnumVar = interopClass("CEnumVar")

    val nativeMemUtils = symbolTable.referenceClass(context.interopBuiltIns.nativeMemUtils)

    val readBits = interopFunction("readBits")
    val writeBits = interopFunction("writeBits")

    val objCExportTrapOnUndeclaredException =
            symbolTable.referenceSimpleFunction(context.builtIns.kotlinNativeInternal.getContributedFunctions(
                    Name.identifier("trapOnUndeclaredException"),
                    NoLookupLocation.FROM_BACKEND
            ).single())

    val objCExportResumeContinuation = internalFunction("resumeContinuation")
    val objCExportResumeContinuationWithException = internalFunction("resumeContinuationWithException")
    val objCExportGetCoroutineSuspended = internalFunction("getCoroutineSuspended")
    val objCExportInterceptedContinuation = internalFunction("interceptedContinuation")

    val getNativeNullPtr = symbolTable.referenceSimpleFunction(context.getNativeNullPtr)

    val boxCachePredicates = BoxCache.values().associate {
        it to internalFunction("in${it.name.toLowerCase().capitalize()}BoxCache")
    }

    val boxCacheGetters = BoxCache.values().associate {
        it to internalFunction("getCached${it.name.toLowerCase().capitalize()}Box")
    }

    val immutableBlob = symbolTable.referenceClass(
            builtInsPackage("kotlin", "native").getContributedClassifier(
                    Name.identifier("ImmutableBlob"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    val executeImpl = symbolTable.referenceSimpleFunction(
            builtIns.builtInsModule.getPackage(FqName("kotlin.native.concurrent")).memberScope
                    .getContributedFunctions(Name.identifier("executeImpl"), NoLookupLocation.FROM_BACKEND)
                    .single()
    )

    val areEqualByValue = context.getKonanInternalFunctions("areEqualByValue").map {
        symbolTable.referenceSimpleFunction(it)
    }.associateBy { it.descriptor.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()!! }

    val reinterpret = internalFunction("reinterpret")

    val ieee754Equals = context.getKonanInternalFunctions("ieee754Equals").map {
        symbolTable.referenceSimpleFunction(it)
    }

    val equals = context.builtIns.any.unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier("equals"), NoLookupLocation.FROM_BACKEND)
            .single().let { symbolTable.referenceSimpleFunction(it) }

    val throwArithmeticException = internalFunction("ThrowArithmeticException")

    val throwIndexOutOfBoundsException = internalFunction("ThrowIndexOutOfBoundsException")

    override val throwNullPointerException = internalFunction("ThrowNullPointerException")

    override val throwNoWhenBranchMatchedException = internalFunction("ThrowNoWhenBranchMatchedException")

    override val throwTypeCastException = internalFunction("ThrowTypeCastException")

    override val throwKotlinNothingValueException  = internalFunction("ThrowKotlinNothingValueException")

    val throwClassCastException = internalFunction("ThrowClassCastException")

    val throwInvalidReceiverTypeException = internalFunction("ThrowInvalidReceiverTypeException")
    val throwIllegalStateException = internalFunction("ThrowIllegalStateException")
    val throwIllegalStateExceptionWithMessage = internalFunction("ThrowIllegalStateExceptionWithMessage")
    val throwIllegalArgumentException = internalFunction("ThrowIllegalArgumentException")
    val throwIllegalArgumentExceptionWithMessage = internalFunction("ThrowIllegalArgumentExceptionWithMessage")


    override val throwUninitializedPropertyAccessException = internalFunction("ThrowUninitializedPropertyAccessException")

    override val stringBuilder = symbolTable.referenceClass(
            builtInsPackage("kotlin", "text").getContributedClassifier(
                    Name.identifier("StringBuilder"), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor
    )

    override val defaultConstructorMarker = symbolTable.referenceClass(
            context.getKonanInternalClass("DefaultConstructorMarker")
    )

    val checkProgressionStep = context.getKonanInternalFunctions("checkProgressionStep")
            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()
    val getProgressionLast = context.getKonanInternalFunctions("getProgressionLast")
            .map { Pair(it.returnType, symbolTable.referenceSimpleFunction(it)) }.toMap()

    val arrayContentToString = arrays.associateBy(
            { it },
            { findArrayExtension(it.descriptor, "contentToString") }
    )
    val arrayContentHashCode = arrays.associateBy(
            { it },
            { findArrayExtension(it.descriptor, "contentHashCode") }
    )

    private val kotlinCollectionsPackageScope: MemberScope
        get() = builtInsPackage("kotlin", "collections")

    private fun findArrayExtension(descriptor: ClassDescriptor, name: String): IrSimpleFunctionSymbol {
        val functionDescriptor = kotlinCollectionsPackageScope
                .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                .singleOrNull {
                    it.valueParameters.isEmpty()
                            && it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == descriptor
                            && it.extensionReceiverParameter?.type?.isMarkedNullable == false
                            && !it.isExpect
                } ?: error(descriptor.toString())
        return symbolTable.referenceSimpleFunction(functionDescriptor)
    }
    override val copyRangeTo get() = TODO()

    fun getNoParamFunction(name: Name, receiverType: KotlinType): IrFunctionSymbol {
        val descriptor = receiverType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
                .first { it.valueParameters.isEmpty() }
        return symbolTable.referenceFunction(descriptor)
    }
    
    val copyInto = arrays.map { symbol ->
        val packageViewDescriptor = builtIns.builtInsModule.getPackage(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME)
        val functionDescriptor = packageViewDescriptor.memberScope
                .getContributedFunctions(Name.identifier("copyInto"), NoLookupLocation.FROM_BACKEND)
                .single {
                    !it.isExpect &&
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


    val valuesForEnum = internalFunction("valuesForEnum")

    val valueOfForEnum = internalFunction("valueOfForEnum")

    val createUninitializedInstance = internalFunction("createUninitializedInstance")

    val initInstance = internalFunction("initInstance")

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

    override val getContinuation = internalFunction("getContinuation")

    override val returnIfSuspended = internalFunction("returnIfSuspended")

    val coroutineLaunchpad = internalFunction("coroutineLaunchpad")

    override val suspendCoroutineUninterceptedOrReturn = internalFunction("suspendCoroutineUninterceptedOrReturn")

    private val coroutinesIntrinsicsPackage = context.builtIns.builtInsModule.getPackage(
        context.config.configuration.languageVersionSettings.coroutinesIntrinsicsPackageFqName()).memberScope

    private val coroutinesPackage = context.builtIns.builtInsModule.getPackage(
            context.config.configuration.languageVersionSettings.coroutinesPackageFqName()).memberScope

    override val coroutineContextGetter = symbolTable.referenceSimpleFunction(
            coroutinesPackage
                    .getContributedVariables(Name.identifier("coroutineContext"), NoLookupLocation.FROM_BACKEND)
                    .single()
                    .getter!!)

    override val coroutineGetContext = internalFunction("getCoroutineContext")

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = topLevelClass("kotlin.coroutines.native.internal.BaseContinuationImpl")

    val restrictedContinuationImpl = topLevelClass("kotlin.coroutines.native.internal.RestrictedContinuationImpl")

    val continuationImpl = topLevelClass("kotlin.coroutines.native.internal.ContinuationImpl")

    val invokeSuspendFunction =
            symbolTable.referenceSimpleFunction(
                    baseContinuationImpl.descriptor.unsubstitutedMemberScope
                            .getContributedFunctions(Name.identifier("invokeSuspend"), NoLookupLocation.FROM_BACKEND)
                            .single()
            )

    override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            coroutinesIntrinsicsPackage
                    .getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND)
                    .filterNot { it.isExpect }.single().getter!!
    )

    val cancellationException = topLevelClass(KonanFqNames.cancellationException)

    val kotlinResult = topLevelClass("kotlin.Result")

    val kotlinResultGetOrThrow = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin")
                    .getContributedFunctions(Name.identifier("getOrThrow"), NoLookupLocation.FROM_BACKEND)
                    .single {
                        it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == kotlinResult.descriptor
                    }
    )

    override val functionAdapter = symbolTable.referenceClass(context.getKonanInternalClass("FunctionAdapter"))

    val refClass = symbolTable.referenceClass(context.getKonanInternalClass("Ref"))

    val kFunctionImpl =  symbolTable.referenceClass(context.reflectionTypes.kFunctionImpl)
    val kSuspendFunctionImpl =  symbolTable.referenceClass(context.reflectionTypes.kSuspendFunctionImpl)

    val kMutableProperty0 = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty0)
    val kMutableProperty1 = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty1)
    val kMutableProperty2 = symbolTable.referenceClass(context.reflectionTypes.kMutableProperty2)

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

    val kTypeProjection = symbolTable.referenceClass(context.reflectionTypes.kTypeProjection)

    private val kTypeProjectionCompanionDescriptor = context.reflectionTypes.kTypeProjection.companionObjectDescriptor!!

    val kTypeProjectionCompanion = symbolTable.referenceClass(kTypeProjectionCompanionDescriptor)

    val kTypeProjectionStar = symbolTable.referenceProperty(
            kTypeProjectionCompanionDescriptor.unsubstitutedMemberScope
                    .getContributedVariables(Name.identifier("STAR"), NoLookupLocation.FROM_BACKEND).single()
    )

    val kTypeProjectionFactories: Map<Variance, IrSimpleFunctionSymbol> = Variance.values().toList().associateWith {
        val factoryName = when (it) {
            Variance.INVARIANT -> "invariant"
            Variance.IN_VARIANCE -> "contravariant"
            Variance.OUT_VARIANCE -> "covariant"
        }

        symbolTable.referenceSimpleFunction(
                kTypeProjectionCompanionDescriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier(factoryName), NoLookupLocation.FROM_BACKEND).single()
        )
    }

    val emptyList = symbolTable.referenceSimpleFunction(
            kotlinCollectionsPackageScope
                    .getContributedFunctions(Name.identifier("emptyList"), NoLookupLocation.FROM_BACKEND)
                    .single { it.valueParameters.isEmpty() }
    )

    val listOf = symbolTable.referenceSimpleFunction(
            kotlinCollectionsPackageScope
                    .getContributedFunctions(Name.identifier("listOf"), NoLookupLocation.FROM_BACKEND)
                    .single { it.valueParameters.size == 1 && it.valueParameters[0].isVararg }
    )
    val listOfInternal = internalFunction("listOfInternal")

    val threadLocal = symbolTable.referenceClass(
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.threadLocal))!!)

    val sharedImmutable = symbolTable.referenceClass(
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.sharedImmutable))!!)

    private fun topLevelClass(fqName: String): IrClassSymbol = topLevelClass(FqName(fqName))
    private fun topLevelClass(fqName: FqName): IrClassSymbol = classById(ClassId.topLevel(fqName))
    private fun classById(classId: ClassId): IrClassSymbol =
            symbolTable.referenceClass(builtIns.builtInsModule.findClassAcrossModuleDependencies(classId)!!)

    private fun internalFunction(name: String): IrSimpleFunctionSymbol =
            symbolTable.referenceSimpleFunction(context.getKonanInternalFunctions(name).single())

    private fun internalClass(name: String): IrClassSymbol =
            symbolTable.referenceClass(context.getKonanInternalClass(name))

    private fun getKonanTestClass(className: String) = symbolTable.referenceClass(
            builtInsPackage("kotlin", "native", "internal", "test").getContributedClassifier(
                    Name.identifier(className), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor)

    private fun interopFunction(name: String) = symbolTable.referenceSimpleFunction(
            context.interopBuiltIns.packageScope
                    .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                    .single()
    )

    private fun interopClass(name: String) = symbolTable.referenceClass(
            context.interopBuiltIns.packageScope
                    .getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    )

    override fun functionN(n: Int) = functionIrClassFactory.functionN(n).symbol

    override fun suspendFunctionN(n: Int) = functionIrClassFactory.suspendFunctionN(n).symbol

    fun kFunctionN(n: Int) = functionIrClassFactory.kFunctionN(n).symbol

    fun kSuspendFunctionN(n: Int) = functionIrClassFactory.kSuspendFunctionN(n).symbol

    fun getKFunctionType(returnType: IrType, parameterTypes: List<IrType>) =
            kFunctionN(parameterTypes.size).typeWith(parameterTypes + returnType)

    val baseClassSuite   = getKonanTestClass("BaseClassSuite")
    val topLevelSuite    = getKonanTestClass("TopLevelSuite")
    val testFunctionKind = getKonanTestClass("TestFunctionKind")

    private val testFunctionKindCache = TestProcessor.FunctionKind.values().associate {
        val symbol = if (it.runtimeKindString.isEmpty())
            null
        else
            symbolTable.referenceEnumEntry(testFunctionKind.descriptor.unsubstitutedMemberScope.getContributedClassifier(
                    Name.identifier(it.runtimeKindString), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor)
        it to symbol
    }

    fun getTestFunctionKind(kind: TestProcessor.FunctionKind) = testFunctionKindCache[kind]!!
}

private fun getArrayListClassDescriptor(context: Context): ClassDescriptor {
    val module = context.builtIns.builtInsModule
    val pkg = module.getPackage(FqName.fromSegments(listOf("kotlin", "collections")))
    val classifier = pkg.memberScope.getContributedClassifier(Name.identifier("ArrayList"),
            NoLookupLocation.FROM_BACKEND)

    return classifier as ClassDescriptor
}
