/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance

class JvmSymbols(
    context: JvmBackendContext,
    private val symbolTable: ReferenceSymbolTable
) : Symbols<JvmBackendContext>(context, symbolTable) {
    private val storageManager = LockBasedStorageManager(this::class.java.simpleName)
    private val kotlinPackage: IrPackageFragment = createPackage(FqName("kotlin"))
    private val kotlinCoroutinesPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines"))
    private val kotlinCoroutinesJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines.jvm.internal"))
    private val kotlinJvmPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm"))
    private val kotlinJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.internal"))
    private val kotlinJvmFunctionsPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.functions"))
    private val javaLangPackage: IrPackageFragment = createPackage(FqName("java.lang"))

    private val irBuiltIns = context.irBuiltIns

    private val nullPointerExceptionClass: IrClassSymbol =
        createClass(FqName("java.lang.NullPointerException")) { klass ->
            klass.addConstructor().apply {
                addValueParameter("message", irBuiltIns.stringType)
            }
        }

    override val ThrowNullPointerException: IrFunctionSymbol =
        nullPointerExceptionClass.constructors.single()

    override val ThrowNoWhenBranchMatchedException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    private val typeCastExceptionClass: IrClassSymbol =
        createClass(FqName("kotlin.TypeCastException")) { klass ->
            klass.addConstructor().apply {
                addValueParameter("message", irBuiltIns.stringType)
            }
        }

    override val ThrowTypeCastException: IrFunctionSymbol =
        typeCastExceptionClass.constructors.single()

    val unsupportedOperationExceptionClass: IrClassSymbol = createClass(FqName("java.lang.UnsupportedOperationException")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("message", irBuiltIns.stringType.makeNullable())
        }
    }

    private fun createPackage(fqName: FqName): IrPackageFragment =
        IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(EmptyPackageFragmentDescriptor(context.state.module, fqName)))

    private fun createClass(
        fqName: FqName,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.FINAL,
        classIsInline: Boolean = false,
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol =
        buildClass {
            name = fqName.shortName()
            kind = classKind
            modality = classModality
            isInline = classIsInline
        }.apply {
            parent = when (fqName.parent().asString()) {
                "kotlin" -> kotlinPackage
                "kotlin.coroutines" -> kotlinCoroutinesPackage
                "kotlin.coroutines.jvm.internal" -> kotlinCoroutinesJvmInternalPackage
                "kotlin.jvm.internal" -> kotlinJvmInternalPackage
                "kotlin.jvm.functions" -> kotlinJvmFunctionsPackage
                "java.lang" -> javaLangPackage
                else -> error("Other packages are not supported yet: $fqName")
            }
            createImplicitParameterDeclarationWithWrappedDescriptor()
            block(this)
        }.symbol

    private val intrinsicsClass: IrClassSymbol = createClass(
        JvmClassName.byInternalName(IrIntrinsicMethods.INTRINSICS_CLASS_NAME).fqNameForTopLevelClassMaybeWithDollars
    ) { klass ->
        klass.addFunction("throwUninitializedPropertyAccessException", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("propertyName", irBuiltIns.stringType)
        }
        klass.addFunction("checkExpressionValueIsNotNull", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("value", irBuiltIns.anyNType)
            addValueParameter("expression", irBuiltIns.stringType)
        }
        klass.addFunction("checkNotNullExpressionValue", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("value", irBuiltIns.anyNType)
            addValueParameter("expression", irBuiltIns.stringType)
        }
        klass.addFunction("stringPlus", irBuiltIns.stringType, isStatic = true).apply {
            addValueParameter("self", irBuiltIns.stringType.makeNullable())
            addValueParameter("other", irBuiltIns.anyNType)
        }
    }

    val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkExpressionValueIsNotNull" }

    val checkNotNullExpressionValue: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkNotNullExpressionValue" }

    override val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwUninitializedPropertyAccessException" }

    val intrinsicStringPlus: IrFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "stringPlus" }

    override val stringBuilder: IrClassSymbol = createClass(FqName("java.lang.StringBuilder")) { klass ->
        klass.addConstructor()
        klass.addFunction("toString", irBuiltIns.stringType).apply {
            overriddenSymbols += any.functionByName("toString")
        }

        val appendTypes = with(irBuiltIns) { listOf(anyNType, stringType, booleanType, charType, intType, longType, floatType, doubleType) }
        for (type in appendTypes) {
            klass.addFunction("append", klass.defaultType).apply {
                addValueParameter("value", type)
            }
        }
    }

    override val defaultConstructorMarker: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.DefaultConstructorMarker"))

    override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
        get() = error("Unused in JVM IR")

    override val coroutineImpl: IrClassSymbol
        get() = TODO("not implemented")

    override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    override val getContinuation: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    override val coroutineContextGetter: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    override val coroutineGetContext: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    override val returnIfSuspended: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    val javaLangClass: IrClassSymbol =
        createClass(FqName("java.lang.Class")) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.INVARIANT)
            klass.addFunction("desiredAssertionStatus", irBuiltIns.booleanType)
        }

    private val javaLangAssertionError: IrClassSymbol =
        createClass(FqName("java.lang.AssertionError")) { klass ->
            klass.addConstructor().apply {
                addValueParameter("detailMessage", irBuiltIns.anyNType)
            }
        }

    val assertionErrorConstructor = javaLangAssertionError.constructors.single()

    val continuationClass: IrClassSymbol =
        createClass(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE, ClassKind.INTERFACE) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.IN_VARIANCE)
        }

    private val resultClassStub: IrClassSymbol =
        createClass(DescriptorUtils.RESULT_FQ_NAME, classIsInline = true) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.OUT_VARIANCE)
            klass.addConstructor { isPrimary = true }.apply {
                addValueParameter("value", irBuiltIns.anyNType)
            }
        }

    val continuationImplClass: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.ContinuationImpl"), classModality = Modality.ABSTRACT) { klass ->
            val continuationType = continuationClass.typeWith(irBuiltIns.anyNType)
            klass.superTypes += continuationType
            klass.addConstructor().apply {
                addValueParameter("completion", continuationType.makeNullable())
            }
            klass.addFunction("invokeSuspend", irBuiltIns.anyNType, Modality.ABSTRACT).apply {
                addValueParameter("result", resultClassStub.typeWith(irBuiltIns.anyNType))
            }
        }

    val suspendFunctionInterface: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.SuspendFunction"), ClassKind.INTERFACE)

    val lambdaClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Lambda")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
        }
    }

    val suspendLambdaClass: IrClassSymbol = createClass(FqName("kotlin.coroutines.jvm.internal.SuspendLambda")) { klass ->
        klass.superTypes += suspendFunctionInterface.typeWith()
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
            addValueParameter("completion", continuationClass.typeWith(irBuiltIns.anyNType).makeNullable())
        }
        klass.addFunction("invokeSuspend", irBuiltIns.anyNType, Modality.ABSTRACT).apply {
            addValueParameter("result", resultClassStub.typeWith(irBuiltIns.anyNType))
        }
        klass.addFunction("create", continuationClass.typeWith(irBuiltIns.unitType)).apply {
            addValueParameter("completion", continuationClass.typeWith(irBuiltIns.nothingType))
        }
        klass.addFunction("create", continuationClass.typeWith(irBuiltIns.unitType)).apply {
            addValueParameter("value", irBuiltIns.anyNType)
            addValueParameter("completion", continuationClass.typeWith(irBuiltIns.nothingType))
        }
    }

    private fun generateCallableReferenceMethods(klass: IrClass) {
        klass.addFunction("getSignature", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getName", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getOwner", irBuiltIns.kDeclarationContainerClass.typeWith(), Modality.OPEN)
    }

    val functionReference: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.FunctionReference")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
        }

        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
            addValueParameter("receiver", irBuiltIns.anyNType)
        }

        klass.addField("receiver", irBuiltIns.anyNType, Visibilities.PROTECTED)

        generateCallableReferenceMethods(klass)
    }

    val functionReferenceReceiverField: IrFieldSymbol = functionReference.fieldByName("receiver")
    val functionReferenceGetSignature: IrSimpleFunctionSymbol = functionReference.functionByName("getSignature")
    val functionReferenceGetName: IrSimpleFunctionSymbol = functionReference.functionByName("getName")
    val functionReferenceGetOwner: IrSimpleFunctionSymbol = functionReference.functionByName("getOwner")

    fun getFunction(parameterCount: Int): IrClassSymbol =
        symbolTable.referenceClass(builtIns.getFunction(parameterCount))

    private val jvmFunctionClasses = storageManager.createMemoizedFunction { n: Int ->
        createFunctionClass(n, false)
    }

    private fun createFunctionClass(n: Int, isSuspend: Boolean): IrClassSymbol =
        createClass(FqName("kotlin.jvm.functions.Function${n + if (isSuspend) 1 else 0}"), ClassKind.INTERFACE) { klass ->
            for (i in 1..n) {
                klass.addTypeParameter("P$i", irBuiltIns.anyNType, Variance.IN_VARIANCE)
            }
            val returnType = klass.addTypeParameter("R", irBuiltIns.anyNType, Variance.OUT_VARIANCE)

            klass.addFunction("invoke", returnType.defaultType, Modality.ABSTRACT, isSuspend = isSuspend).apply {
                for (i in 1..n) {
                    addValueParameter("p$i", klass.typeParameters[i - 1].defaultType)
                }
            }
        }

    fun getJvmFunctionClass(parameterCount: Int): IrClassSymbol =
        jvmFunctionClasses(parameterCount)

    private val jvmSuspendFunctionClasses = storageManager.createMemoizedFunction { n: Int ->
        createFunctionClass(n, true)
    }

    fun getJvmSuspendFunctionClass(parameterCount: Int): IrClassSymbol =
        jvmSuspendFunctionClasses(parameterCount)

    val functionN: IrClassSymbol = createClass(FqName("kotlin.jvm.functions.FunctionN"), ClassKind.INTERFACE) { klass ->
        val returnType = klass.addTypeParameter("R", irBuiltIns.anyNType, Variance.OUT_VARIANCE)

        klass.addFunction("invoke", returnType.defaultType, Modality.ABSTRACT).apply {
            addValueParameter {
                name = Name.identifier("args")
                type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
                origin = IrDeclarationOrigin.DEFINED
                varargElementType = irBuiltIns.anyNType
            }
        }
    }

    private data class PropertyReferenceKey(
        val mutable: Boolean,
        val parameterCount: Int,
        val impl: Boolean
    )

    private val propertyReferenceClasses = storageManager.createMemoizedFunction { key: PropertyReferenceKey ->
        val (mutable, n, impl) = key
        val className = buildString {
            if (mutable) append("Mutable")
            append("PropertyReference")
            append(n)
            if (impl) append("Impl")
        }
        createClass(FqName("kotlin.jvm.internal.$className")) { klass ->
            if (impl) {
                klass.addConstructor().apply {
                    addValueParameter("owner", irBuiltIns.kDeclarationContainerClass.typeWith())
                    addValueParameter("name", irBuiltIns.stringType)
                    addValueParameter("string", irBuiltIns.stringType)
                }
            } else {
                klass.addConstructor()

                klass.addConstructor().apply {
                    addValueParameter("receiver", irBuiltIns.anyNType)
                }
            }

            val receiverFieldName = Name.identifier("receiver")
            klass.addProperty {
                name = receiverFieldName
            }.apply {
                backingField = buildField {
                    name = receiverFieldName
                    type = irBuiltIns.anyNType
                    visibility = Visibilities.PROTECTED
                }.also { field ->
                    field.parent = klass
                }
            }

            generateCallableReferenceMethods(klass)

            // To avoid hassle with generic type parameters, we pretend that PropertyReferenceN.get takes and returns `Any?`
            // (similarly with set). This should be enough for the JVM IR backend to generate correct calls and bridges.
            klass.addFunction("get", irBuiltIns.anyNType, Modality.ABSTRACT).apply {
                for (i in 0 until n) {
                    addValueParameter("receiver$i", irBuiltIns.anyNType)
                }
            }

            // invoke redirects to get
            klass.addFunction("invoke", irBuiltIns.anyNType, Modality.FINAL).apply {
                for (i in 0 until n) {
                    addValueParameter("receiver$i", irBuiltIns.anyNType)
                }
            }

            if (mutable) {
                klass.addFunction("set", irBuiltIns.unitType, Modality.ABSTRACT).apply {
                    for (i in 0 until n) {
                        addValueParameter("receiver$i", irBuiltIns.anyNType)
                    }
                    addValueParameter("value", irBuiltIns.anyNType)
                }
            }
        }
    }

    fun getPropertyReferenceClass(mutable: Boolean, parameterCount: Int, impl: Boolean): IrClassSymbol =
        propertyReferenceClasses(PropertyReferenceKey(mutable, parameterCount, impl))

    val reflection: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Reflection")) { klass ->
        // We use raw types for java.lang.Class and kotlin.reflect.KClass here for simplicity and because this is what's used
        // at declaration site at kotlin.jvm.internal.Reflection.
        val rawJavaLangClass = javaLangClass.typeWith()
        val rawKClass = irBuiltIns.kClassClass.typeWith()

        klass.addFunction("getOrCreateKotlinPackage", irBuiltIns.kDeclarationContainerClass.typeWith(), isStatic = true).apply {
            addValueParameter("javaClass", rawJavaLangClass)
            addValueParameter("moduleName", irBuiltIns.stringType)
        }

        klass.addFunction("getOrCreateKotlinClass", rawKClass, isStatic = true).apply {
            addValueParameter("javaClass", rawJavaLangClass)
        }

        klass.addFunction("getOrCreateKotlinClasses", irBuiltIns.arrayClass.typeWith(rawKClass), isStatic = true).apply {
            addValueParameter("javaClasses", irBuiltIns.arrayClass.typeWith(rawJavaLangClass))
        }

        for (mutable in listOf(false, true)) {
            for (n in 0..2) {
                val functionName = (if (mutable) "mutableProperty" else "property") + n
                klass.addFunction(functionName, irBuiltIns.getKPropertyClass(mutable, n).typeWith(), isStatic = true).apply {
                    addValueParameter("p", getPropertyReferenceClass(mutable, n, impl = false).typeWith())
                }
            }
        }
    }

    val getOrCreateKotlinPackage: IrSimpleFunctionSymbol =
        reflection.functionByName("getOrCreateKotlinPackage")

    val desiredAssertionStatus: IrSimpleFunctionSymbol by lazy {
        javaLangClass.functionByName("desiredAssertionStatus")
    }

    val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol =
        buildFun {
            name = Name.special("<unsafe-coerce>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }.apply {
            parent = kotlinJvmInternalPackage
            val src = addTypeParameter("T", irBuiltIns.anyNType)
            val dst = addTypeParameter("R", irBuiltIns.anyNType)
            addValueParameter("v", src.defaultType)
            returnType = dst.defaultType
        }.symbol

    val reassignParameterIntrinsic: IrSimpleFunctionSymbol =
        buildFun {
            name = Name.special("<set-parameter>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }.apply {
            parent = kotlinJvmInternalPackage
            val type = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("parameter", type.defaultType) // must be IrGetValue of an IrValueParameter
            addValueParameter("value", type.defaultType)
            returnType = irBuiltIns.unitType
        }.symbol

    private val collectionToArrayClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.CollectionToArray")) { klass ->
        klass.origin = JvmLoweredDeclarationOrigin.TO_ARRAY

        val arrayType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
        klass.addFunction("toArray", arrayType, isStatic = true).apply {
            origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            addValueParameter("collection", irBuiltIns.collectionClass.owner.typeWith(), JvmLoweredDeclarationOrigin.TO_ARRAY)
        }
        klass.addFunction("toArray", arrayType, isStatic = true).apply {
            origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            addValueParameter("collection", irBuiltIns.collectionClass.owner.typeWith(), JvmLoweredDeclarationOrigin.TO_ARRAY)
            addValueParameter("array", arrayType, JvmLoweredDeclarationOrigin.TO_ARRAY)
        }
    }

    val nonGenericToArray: IrSimpleFunctionSymbol =
        collectionToArrayClass.functions.single { it.owner.name.asString() == "toArray" && it.owner.valueParameters.size == 1 }

    val genericToArray: IrSimpleFunctionSymbol =
        collectionToArrayClass.functions.single { it.owner.name.asString() == "toArray" && it.owner.valueParameters.size == 2 }

    val kClassJava: IrPropertySymbol =
        buildProperty {
            name = Name.identifier("java")
        }.apply {
            parent = kotlinJvmPackage
            addGetter().apply {
                addExtensionReceiver(irBuiltIns.kClassClass.typeWith())
                returnType = javaLangClass.typeWith()
            }
        }.symbol

    val spreadBuilder: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.SpreadBuilder")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("size", irBuiltIns.intType)
        }

        klass.addFunction("addSpread", irBuiltIns.unitType).apply {
            addValueParameter("container", irBuiltIns.anyNType)
        }

        klass.addFunction("add", irBuiltIns.unitType).apply {
            addValueParameter("element", irBuiltIns.anyNType)
        }

        klass.addFunction("size", irBuiltIns.intType)

        klass.addFunction("toArray", irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)).apply {
            addValueParameter("a", irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType))
        }
    }

    val primitiveSpreadBuilders: Map<IrType, IrClassSymbol> = irBuiltIns.primitiveIrTypes.associateWith { irType ->
        val name = irType.classOrNull!!.owner.name
        createClass(FqName("kotlin.jvm.internal.${name}SpreadBuilder")) { klass ->
            klass.addConstructor().apply {
                addValueParameter("size", irBuiltIns.intType)
            }

            klass.addFunction("addSpread", irBuiltIns.unitType).apply {
                // This is really a generic method in the superclass (PrimitiveSpreadBuilder).
                // That is why the argument type is Object rather than the correct
                // primitive array type.
                addValueParameter("container", irBuiltIns.anyNType)
            }

            klass.addFunction("add", irBuiltIns.unitType).apply {
                addValueParameter("element", irType)
            }

            klass.addFunction("toArray", irBuiltIns.primitiveArrayForType.getValue(irType).owner.typeWith())
        }
    }

    private val systemClass: IrClassSymbol = createClass(FqName("java.lang.System")) { klass ->
        klass.addFunction("arraycopy", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("src", irBuiltIns.anyNType)
            addValueParameter("srcPos", irBuiltIns.intType)
            addValueParameter("dest", irBuiltIns.anyNType)
            addValueParameter("destPos", irBuiltIns.intType)
            addValueParameter("length", irBuiltIns.intType)
        }
    }

    val systemArraycopy: IrSimpleFunctionSymbol = systemClass.functionByName("arraycopy")

    val signatureStringIntrinsic: IrSimpleFunctionSymbol =
        buildFun {
            name = Name.special("<signature-string>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }.apply {
            parent = kotlinJvmInternalPackage
            addValueParameter("v", irBuiltIns.anyNType)
            returnType = irBuiltIns.stringType
        }.symbol

    private val javaLangString: IrClassSymbol =
        createClass(FqName("java.lang.String")) { klass ->
            val valueOfTypes = with(irBuiltIns) {
                listOf(anyNType, booleanType, charType, intType, longType, floatType, doubleType)
            }

            for (type in valueOfTypes) {
                klass.addFunction("valueOf", irBuiltIns.stringType, isStatic = true).apply {
                    addValueParameter("value", type)
                }
            }
        }

    private val defaultValueOfFunction = javaLangString.functions.single {
        it.owner.name.asString() == "valueOf" && it.owner.valueParameters.singleOrNull()?.type?.isNullableAny() == true
    }

    private val valueOfFunctions: Map<IrType, IrSimpleFunctionSymbol?> =
        context.irBuiltIns.primitiveIrTypes.associateWith { type ->
            javaLangString.functions.singleOrNull {
                it.owner.name.asString() == "valueOf" && it.owner.valueParameters.singleOrNull()?.type == type
            }
        }

    fun typeToStringValueOfFunction(type: IrType): IrSimpleFunctionSymbol =
        valueOfFunctions[type] ?: defaultValueOfFunction
}

private fun IrClassSymbol.functionByName(name: String): IrSimpleFunctionSymbol =
    functions.single { it.owner.name.asString() == name }

private fun IrClassSymbol.fieldByName(name: String): IrFieldSymbol =
    fields.single { it.owner.name.asString() == name }
