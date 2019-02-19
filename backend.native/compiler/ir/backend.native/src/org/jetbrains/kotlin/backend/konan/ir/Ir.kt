/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.RenderIrElementWithDescriptorsVisitor.Companion.DECLARATION_RENDERER
import org.jetbrains.kotlin.backend.common.descriptors.WrappedDeclarationDescriptor
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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.StringWriter
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

    val interopObjCRelease = symbolTable.referenceSimpleFunction(
            context.interopBuiltIns.packageScope
                    .getContributedFunctions(Name.identifier("objc_release"), NoLookupLocation.FROM_BACKEND)
                    .single()
    )

    val interopObjCRetain = symbolTable.referenceSimpleFunction(
            context.interopBuiltIns.packageScope
                    .getContributedFunctions(Name.identifier("objc_retain"), NoLookupLocation.FROM_BACKEND)
                    .single()
    )

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

fun IrElement.render() = accept(RenderIrElementVisitor(), null)

fun IrType.render() =
        originalKotlinType?.let {
            DECLARATION_RENDERER.renderType(it)
        } ?: DECLARATION_RENDERER.renderType(this.toKotlinType())

class RenderIrElementVisitor : IrElementVisitor<String, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): String =
            "? ${element::class.java.simpleName}"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
            "? ${declaration::class.java.simpleName} ${declaration.descriptor.ref()}"

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String =
            "MODULE_FRAGMENT name:${declaration.name}"

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: Nothing?): String =
            "EXTERNAL_PACKAGE_FRAGMENT fqName:${declaration.fqName}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
            "FILE fqName:${declaration.fqName} fileName:${declaration.name}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
            "FUN ${declaration.renderOriginIfNonTrivial()}${declaration.renderDeclared()}"

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): String =
            declaration.run {
                "FUN ${renderOriginIfNonTrivial()}" +
                        //"name:$name visibility:$visibility modality:$modality " +
                        "name:$name $descriptor $symbol visibility:$visibility modality:$modality " +
                        renderTypeParameters() + " " +
                        renderValueParameterTypes() + " " +
                        "returnType:${returnType.render()} " +
                        "flags:${renderSimpleFunctionFlags()}"
            }

    private fun renderFlagsList(vararg flags: String?) =
            flags.filterNotNull().joinToString(separator = ",")

    private fun IrSimpleFunction.renderSimpleFunctionFlags(): String =
            renderFlagsList(
                    "tailrec".takeIf { isTailrec },
                    "inline".takeIf { isInline },
                    "external".takeIf { isExternal },
                    "suspend".takeIf { isSuspend }
            )

    private fun IrFunction.renderTypeParameters(): String =
            typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") { it.name.toString() }

    private fun IrFunction.renderValueParameterTypes(): String =
            ArrayList<String>().apply {
                addIfNotNull(dispatchReceiverParameter?.run { "\$this:${type.render()}" })
                addIfNotNull(extensionReceiverParameter?.run { "\$receiver:${type.render()}" })
                valueParameters.mapTo(this) { "${it.name}:${it.type.render()}" }
            }.joinToString(separator = ", ", prefix = "(", postfix = ")")

    override fun visitConstructor(declaration: IrConstructor, data: Nothing?): String =
            declaration.run {
                "CONSTRUCTOR ${renderOriginIfNonTrivial()}" +
                        "visibility:$visibility " +
                        renderTypeParameters() + " " +
                        renderValueParameterTypes() + " " +
                        "returnType:${returnType.render()} " +
                        "flags:${renderConstructorFlags()}"
            }

    private fun IrConstructor.renderConstructorFlags() =
            renderFlagsList(
                    "inline".takeIf { isInline },
                    "external".takeIf { isExternal },
                    "primary".takeIf { isPrimary }
            )

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
            declaration.run {
                "PROPERTY ${renderOriginIfNonTrivial()}" +
                        "name:$name visibility:$visibility modality:$modality " +
                        "flags:${renderPropertyFlags()}"
            }

    private fun IrProperty.renderPropertyFlags() =
            renderFlagsList(
                    "external".takeIf { isExternal },
                    "const".takeIf { isConst },
                    "lateinit".takeIf { isLateinit },
                    "delegated".takeIf { isDelegated },
                    if (isVar) "var" else "val"
            )

    override fun visitField(declaration: IrField, data: Nothing?): String =
            "FIELD ${declaration.renderOriginIfNonTrivial()}" +
                    "name:${declaration.name} type:${declaration.type.render()} visibility:${declaration.visibility} " +
                    "flags:${declaration.renderFieldFlags()}"

    private fun IrField.renderFieldFlags() =
            renderFlagsList(
                    "final".takeIf { isFinal },
                    "external".takeIf { isExternal },
                    "static".takeIf { isStatic }
            )

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
            declaration.run {
                "CLASS ${renderOriginIfNonTrivial()}" +
                        "$kind name:$name modality:$modality visibility:$visibility " +
                        "flags:${renderClassFlags()} " +
                        "superTypes:[${superTypes.joinToString(separator = "; ") { it.render() }}]"
            }

    private fun IrClass.renderClassFlags() =
            renderFlagsList(
                    "companion".takeIf { isCompanion },
                    "inner".takeIf { isInner },
                    "data".takeIf { isData },
                    "external".takeIf { isExternal },
                    "inline".takeIf { isInline }
            )

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String =
            "TYPEALIAS ${declaration.renderOriginIfNonTrivial()}${declaration.descriptor.ref()} " +
                    "type=${declaration.descriptor.underlyingType.render()}"

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String =
            "VAR ${declaration.renderOriginIfNonTrivial()}" +
                    "name:${declaration.name} type:${declaration.type.render()} flags:${declaration.renderVariableFlags()}"

    private fun IrVariable.renderVariableFlags(): String =
            renderFlagsList(
                    "const".takeIf { isConst },
                    "lateinit".takeIf { isLateinit },
                    if (isVar) "var" else "val"
            )

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
            "ENUM_ENTRY ${declaration.renderOriginIfNonTrivial()}name:${declaration.name}"

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String =
            "ANONYMOUS_INITIALIZER ${declaration.descriptor.ref()}"

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?): String =
            declaration.run {
                "TYPE_PARAMETER ${renderOriginIfNonTrivial()}" +
                        "name:$name index:$index variance:$variance " +
                        "superTypes:[${superTypes.joinToString(separator = "; ") { it.render() }}]"
            }

    override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): String =
            declaration.run {
                "VALUE_PARAMETER ${renderOriginIfNonTrivial()}" +
                        "name:$name " +
                        (if (index >= 0) "index:$index " else "") +
                        "type:${type.render()} " +
                        (varargElementType?.let { "varargElementType:${it.render()} " } ?: "") +
                        "flags:${renderValueParameterFlags()}"
            }

    private fun IrValueParameter.renderValueParameterFlags(): String =
            renderFlagsList(
                    "vararg".takeIf { varargElementType != null },
                    "crossinline".takeIf { isCrossinline },
                    "noinline".takeIf { isNoinline }
            )

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
            declaration.run {
                "LOCAL_DELEGATED_PROPERTY ${declaration.renderOriginIfNonTrivial()}" +
                        "name:$name type:${type.render()} flags:${renderLocalDelegatedPropertyFlags()}"
            }

    private fun IrLocalDelegatedProperty.renderLocalDelegatedPropertyFlags() =
            if (isVar) "var" else "val"

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
            "EXPRESSION_BODY"

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String =
            "BLOCK_BODY"

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): String =
            "SYNTHETIC_BODY kind=${body.kind}"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
            "? ${expression::class.java.simpleName} type=${expression.type.render()}"

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): String =
            "CONST ${expression.kind} type=${expression.type.render()} value=${expression.value}"

    override fun visitVararg(expression: IrVararg, data: Nothing?): String =
            "VARARG type=${expression.type.render()} varargElementType=${expression.varargElementType.render()}"

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String =
            "SPREAD_ELEMENT"

    override fun visitBlock(expression: IrBlock, data: Nothing?): String =
            if (expression is IrReturnableBlock)
                "RETURNABLE_BLOCK type=${expression.type.render()} origin=${expression.origin} function=name:${expression.symbol.descriptor.name} ${expression.symbol.descriptor} ${expression.symbol}"
            else "BLOCK type=${expression.type.render()} origin=${expression.origin}"

    override fun visitComposite(expression: IrComposite, data: Nothing?): String =
            "COMPOSITE type=${expression.type.render()} origin=${expression.origin}"

    override fun visitReturn(expression: IrReturn, data: Nothing?): String =
            "RETURN type=${expression.type.render()} from='name=${expression.returnTarget.name} ${expression.returnTarget/*.ref()*/} ${expression.returnTargetSymbol}'"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
            "CALL '${expression.descriptor/*.ref()*/} ${expression.descriptor} ${expression.symbol}' ${expression.renderSuperQualifier()}" +
                    "type=${expression.type.render()} origin=${expression.origin}"

    private fun IrCall.renderSuperQualifier(): String =
            superQualifier?.let { "superQualifier=${it.name} " } ?: ""

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
            "DELEGATING_CONSTRUCTOR_CALL '${expression.descriptor.ref()}'"

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
            "ENUM_CONSTRUCTOR_CALL '${expression.descriptor.ref()}'"

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String =
            "INSTANCE_INITIALIZER_CALL classDescriptor='${expression.classDescriptor.ref()}'"

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String =
            "GET_VAR '${expression.descriptor.ref()}' /*${expression.symbol.owner} ${expression.symbol.owner.hashCode()} ${expression.descriptor} ${expression.descriptor.hashCode()} */ type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String =
            "SET_VAR '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetField(expression: IrGetField, data: Nothing?): String =
            "GET_FIELD '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetField(expression: IrSetField, data: Nothing?): String =
            "SET_FIELD '${expression.descriptor.ref()}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String =
            "GET_OBJECT '${expression.descriptor.ref()}' type=${expression.type.render()}"

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String =
            "GET_ENUM '${expression.descriptor.ref()}' type=${expression.type.render()}"

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): String =
            "STRING_CONCATENATION type=${expression.type.render()}"

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String =
            "TYPE_OP type=${expression.type.render()} origin=${expression.operator} typeOperand=${expression.typeOperand.render()}"

    override fun visitWhen(expression: IrWhen, data: Nothing?): String =
            "WHEN type=${expression.type.render()} origin=${expression.origin}"

    override fun visitBranch(branch: IrBranch, data: Nothing?): String =
            "BRANCH"

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): String =
            "WHILE label=${loop.label} origin=${loop.origin}"

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): String =
            "DO_WHILE label=${loop.label} origin=${loop.origin}"

    override fun visitBreak(jump: IrBreak, data: Nothing?): String =
            "BREAK label=${jump.label} loop.label=${jump.loop.label}"

    override fun visitContinue(jump: IrContinue, data: Nothing?): String =
            "CONTINUE label=${jump.label} loop.label=${jump.loop.label}"

    override fun visitThrow(expression: IrThrow, data: Nothing?): String =
            "THROW type=${expression.type.render()}"

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): String =
            "FUNCTION_REFERENCE 'name=${expression.descriptor.name/*.ref()*/} ${expression.descriptor} ${expression.symbol}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): String =
            buildString {
                append("PROPERTY_REFERENCE ")
                append("'${expression.descriptor.ref()}' ")
                appendNullableAttribute("field=", expression.field) { "'${it.descriptor.ref()}'" }
                appendNullableAttribute("getter=", expression.getter) { "'${it.descriptor.ref()}'" }
                appendNullableAttribute("setter=", expression.setter) { "'${it.descriptor.ref()}'" }
                append("type=${expression.type.render()} ")
                append("origin=${expression.origin}")
            }

    private inline fun <T : Any> StringBuilder.appendNullableAttribute(prefix: String, value: T?, toString: (T) -> String) {
        append(prefix)
        if (value != null) {
            append(toString(value))
        } else {
            append("null")
        }
        append(" ")
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: Nothing?): String =
            buildString {
                append("LOCAL_DELEGATED_PROPERTY_REFERENCE ")
                append("'${expression.descriptor.ref()}' ")
                append("delegate='${expression.delegate.descriptor.ref()}' ")
                append("getter='${expression.getter.descriptor.ref()}' ")
                appendNullableAttribute("setter=", expression.setter) { "'${it.descriptor.ref()}'" }
                append("type=${expression.type.render()} ")
                append("origin=${expression.origin}")
            }

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String =
            "CLASS_REFERENCE '${expression.descriptor.ref()}' type=${expression.type.render()}"

    override fun visitGetClass(expression: IrGetClass, data: Nothing?): String =
            "GET_CLASS type=${expression.type.render()}"

    override fun visitTry(aTry: IrTry, data: Nothing?): String =
            "TRY type=${aTry.type.render()}"

    override fun visitCatch(aCatch: IrCatch, data: Nothing?): String =
            "CATCH parameter=${aCatch.parameter.ref()}"

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String =
            "ERROR_DECL ${declaration.descriptor::class.java.simpleName} ${declaration.descriptor.ref()}"

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String =
            "ERROR_EXPR '${expression.description}' type=${expression.type.render()}"

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String =
            "ERROR_CALL '${expression.description}' type=${expression.type.render()}"

    companion object {
        val DECLARATION_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = false
            modifiers = DescriptorRendererModifier.ALL
        }

        val REFERENCE_RENDERER = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES

        internal fun IrDeclaration.name(): String =
                descriptor.name.toString()

        internal fun DescriptorRenderer.renderDescriptor(descriptor: DeclarationDescriptor): String =
                if (descriptor is ReceiverParameterDescriptor)
                    "this@${if (descriptor is WrappedDeclarationDescriptor<*>) "Wrapped" else descriptor.containingDeclaration.name}: ${if (descriptor is WrappedDeclarationDescriptor<*>) "Wrapped"  else descriptor.type}"
                else
                    render(descriptor)

        internal fun IrDeclaration.renderDeclared(): String =
                DECLARATION_RENDERER.renderDescriptor(this.descriptor)

        internal fun DeclarationDescriptor.ref(): String =
                REFERENCE_RENDERER.renderDescriptor(this)

        internal fun KotlinType.render(): String =
                DECLARATION_RENDERER.renderType(this)

        internal fun IrDeclaration.renderOriginIfNonTrivial(): String =
                if (origin != IrDeclarationOrigin.DEFINED) origin.toString() + " " else ""
    }
}


class DumpIrTreeVizitor(out: Appendable) : IrElementVisitor<Unit, String> {

    private val printer = Printer(out, "  ")
    private val elementRenderer = RenderIrElementVisitor()

    companion object {
        val ANNOTATIONS_RENDERER = DescriptorRenderer.withOptions {
            verbose = true
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
        }
    }

    override fun visitElement(element: IrElement, data: String) {
        element.dumpLabeledElementWith(data) {
            if (element is IrAnnotationContainer) {
                dumpAnnotations(element)
            }
            element.acceptChildren(this@DumpIrTreeVizitor, "")
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.files.dumpElements()
        }
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.fileAnnotations.dumpItemsWith("fileAnnotations") {
                ANNOTATIONS_RENDERER.renderAnnotation(it)
            }
            dumpAnnotations(declaration)
            declaration.declarations.dumpElements()
        }
    }

    override fun visitClass(declaration: IrClass, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.thisReceiver?.accept(this, "\$this")
            declaration.typeParameters.dumpElements()
            declaration.declarations.dumpElements()
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.correspondingProperty?.dumpInternal("correspondingProperty")
            declaration.overriddenSymbols.dumpItems<IrSymbol>("overridden") {
                it.dumpDeclarationElementOrDescriptor()
            }
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$this")
            declaration.extensionReceiverParameter?.accept(this, "\$receiver")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    private fun dumpAnnotations(element: IrAnnotationContainer) {
        element.annotations.dumpItems("annotations") {
            element.annotations.dumpElements()
        }
    }

    private fun IrSymbol.dumpDeclarationElementOrDescriptor(label: String? = null) {
        when {
            isBound ->
                owner.dumpInternal(label)
            label != null ->
                printer.println("$label: ", "UNBOUND: ", DescriptorRenderer.COMPACT.render(descriptor))
            else ->
                printer.println("UNBOUND: ", DescriptorRenderer.COMPACT.render(descriptor))
        }
    }

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$outer")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.backingField?.accept(this, "")
            declaration.getter?.accept(this, "")
            declaration.setter?.accept(this, "")
        }
    }

    override fun visitField(declaration: IrField, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.overriddenSymbols.dumpItems("overridden") {
                it.dumpDeclarationElementOrDescriptor()
            }
            declaration.initializer?.accept(this, "")
        }
    }

    private fun List<IrElement>.dumpElements() {
        forEach { it.accept(this@DumpIrTreeVizitor, "") }
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.explicitReceiver?.accept(this, "receiver")
            expression.arguments.dumpElements()
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.initializerExpression?.accept(this, "init")
            declaration.correspondingClass?.accept(this, "class")
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            dumpTypeArguments(expression)
            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
            for (valueParameter in expression.descriptor.valueParameters) {
                expression.getValueArgument(valueParameter.index)?.accept(this, valueParameter.name.asString())
            }
        }
    }

    private fun dumpTypeArguments(expression: IrMemberAccessExpression) {
        for (index in 0 until expression.typeArgumentsCount) {
            printer.println(
                    "${expression.descriptor.renderTypeParameter(index)}: ${expression.renderTypeArgument(index)}"
            )
        }
    }

    private fun CallableDescriptor.renderTypeParameter(index: Int): String {
        val typeParameter = original.typeParameters.getOrNull(index)
        return if (typeParameter != null)
            DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(typeParameter)
        else
            "<`$index>"
    }

    private fun IrMemberAccessExpression.renderTypeArgument(index: Int): String =
            getTypeArgument(index)?.render() ?: "<none>"

    override fun visitGetField(expression: IrGetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
        }
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
            expression.value.accept(this, "value")
        }
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.branches.dumpElements()
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        branch.dumpLabeledElementWith(data) {
            branch.condition.accept(this, "if")
            branch.result.accept(this, "then")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.condition.accept(this, "condition")
            loop.body?.accept(this, "body")
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.body?.accept(this, "body")
            loop.condition.accept(this, "condition")
        }
    }

    override fun visitTry(aTry: IrTry, data: String) {
        aTry.dumpLabeledElementWith(data) {
            aTry.tryResult.accept(this, "try")
            aTry.catches.dumpElements()
            aTry.finallyExpression?.accept(this, "finally")
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.typeOperandClassifier.dumpDeclarationElementOrDescriptor("typeOperand")
            expression.acceptChildren(this, "")
        }
    }

    private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented(body)
    }

    private inline fun <T> Collection<T>.dumpItems(caption: String, renderElement: (T) -> Unit) {
        if (isEmpty()) return
        indented(caption) {
            forEach {
                renderElement(it)
            }
        }
    }

    private inline fun <T> Collection<T>.dumpItemsWith(caption: String, renderElement: (T) -> String) {
        if (isEmpty()) return
        indented(caption) {
            forEach {
                printer.println(renderElement(it))
            }
        }
    }

    private fun IrElement.dumpInternal(label: String? = null) {
        if (label != null) {
            printer.println("$label: ", accept(elementRenderer, null))
        } else {
            printer.println(accept(elementRenderer, null))
        }

    }

    private inline fun indented(label: String, body: () -> Unit) {
        printer.println("$label:")
        indented(body)
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    private fun String.withLabel(label: String) =
            if (label.isEmpty()) this else "$label: $this"
}


fun ir2stringWholezzz(ir: IrElement?): String {
    val strWriter = StringWriter()

    ir?.accept(DumpIrTreeVizitor(strWriter), "")
    return strWriter.toString()
}