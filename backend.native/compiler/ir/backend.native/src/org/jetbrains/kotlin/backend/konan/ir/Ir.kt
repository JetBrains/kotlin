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
import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.config.coroutinesPackageFqName
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
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

    private val isInitializedPropertyDescriptor = builtInsPackage("kotlin")
            .getContributedVariables(Name.identifier("isInitialized"), NoLookupLocation.FROM_BACKEND).single {
                it.extensionReceiverParameter.let {
                    it != null && TypeUtils.getClassDescriptor(it.type) == context.reflectionTypes.kProperty0
                } && !it.isExpect
            }

    override val lateinitIsInitializedPropertyGetter: IrSimpleFunctionSymbol
       = symbolTable.referenceSimpleFunction(isInitializedPropertyDescriptor.getter!!)

    val entryPoint = findMainEntryPoint(context)?.let { symbolTable.referenceSimpleFunction(it) }

    override val externalSymbolTable = lazySymbolTable

    val nothing = symbolTable.referenceClass(builtIns.nothing)
    val throwable = symbolTable.referenceClass(builtIns.throwable)
    val string = symbolTable.referenceClass(builtIns.string)
    val enum = symbolTable.referenceClass(builtIns.enum)
    val nativePtr = symbolTable.referenceClass(context.nativePtr)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())

    private fun unsignedClass(unsignedType: UnsignedType): IrClassSymbol = classById(unsignedType.classId)

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

    val symbolName = topLevelClass(RuntimeNames.symbolName)
    val exportForCppRuntime = topLevelClass(RuntimeNames.exportForCppRuntime)

    val objCMethodImp = symbolTable.referenceClass(context.interopBuiltIns.objCMethodImp)

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

    val interopCPointerGetRawValue = symbolTable.referenceSimpleFunction(context.interopBuiltIns.cPointerGetRawValue)

    val interopAllocObjCObject = symbolTable.referenceSimpleFunction(context.interopBuiltIns.allocObjCObject)

    val interopObjCRelease = interopFunction("objc_release")

    val interopObjCRetain = interopFunction("objc_retain")

    val interopObjcRetainAutoreleaseReturnValue = interopFunction("objc_retainAutoreleaseReturnValue")

    val interopGetObjCClass = symbolTable.referenceSimpleFunction(context.interopBuiltIns.getObjCClass)

    val interopObjCObjectSuperInitCheck =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectSuperInitCheck)

    val interopObjCObjectInitBy = symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectInitBy)

    val interopObjCObjectRawValueGetter =
            symbolTable.referenceSimpleFunction(context.interopBuiltIns.objCObjectRawPtr)

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
    override val copyRangeTo get() = TODO()

    val copyInto = arrays.map { symbol ->
        val packageViewDescriptor = builtIns.builtInsModule.getPackage(KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME)
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

    val returnIfSuspended = symbolTable.referenceSimpleFunction(
            context.getInternalFunctions("returnIfSuspended").single())

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

    val baseContinuationImpl = topLevelClass("kotlin.coroutines.native.internal.BaseContinuationImpl")

    val restrictedContinuationImpl = topLevelClass("kotlin.coroutines.native.internal.RestrictedContinuationImpl")

    val continuationImpl = topLevelClass("kotlin.coroutines.native.internal.ContinuationImpl")

    override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            coroutinesIntrinsicsPackage
                    .getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND)
                    .filterNot { it.isExpect }.single().getter!!
    )

    val kotlinResult = topLevelClass("kotlin.Result")

    val kotlinResultGetOrThrow = symbolTable.referenceSimpleFunction(
            builtInsPackage("kotlin")
                    .getContributedFunctions(Name.identifier("getOrThrow"), NoLookupLocation.FROM_BACKEND)
                    .single {
                        it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor == kotlinResult.descriptor
                    }
    )

    val refClass = symbolTable.referenceClass(context.getInternalClass("Ref"))

    val kFunctionImpl =  symbolTable.referenceClass(context.reflectionTypes.kFunctionImpl)

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

    val listOfInternal = internalFunction("listOfInternal")

    val threadLocal =
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.native.concurrent.ThreadLocal")))!!

    val sharedImmutable =
            context.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(FqName("kotlin.native.concurrent.SharedImmutable")))!!

    private fun topLevelClass(fqName: String): IrClassSymbol = topLevelClass(FqName(fqName))
    private fun topLevelClass(fqName: FqName): IrClassSymbol = classById(ClassId.topLevel(fqName))
    private fun classById(classId: ClassId): IrClassSymbol =
            symbolTable.referenceClass(builtIns.builtInsModule.findClassAcrossModuleDependencies(classId)!!)

    private fun internalFunction(name: String): IrSimpleFunctionSymbol =
            symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())

    private fun internalClass(name: String): IrClassSymbol =
            symbolTable.referenceClass(context.getInternalClass(name))

    private fun getKonanTestClass(className: String) = symbolTable.referenceClass(
            builtInsPackage("kotlin", "native", "internal", "test").getContributedClassifier(
                    Name.identifier(className), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor)

    private fun interopFunction(name: String) = symbolTable.referenceSimpleFunction(
            context.interopBuiltIns.packageScope
                    .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                    .single()
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