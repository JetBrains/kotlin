/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PropertyName")

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_CALL_RESULT_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_CREATE_METHOD_NAME
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance

class JvmSymbols(
    context: JvmBackendContext,
    symbolTable: SymbolTable
) : Symbols<JvmBackendContext>(context, context.irBuiltIns, symbolTable) {
    private val storageManager = LockBasedStorageManager(this::class.java.simpleName)
    private val irFactory = context.irFactory

    private val kotlinPackage: IrPackageFragment = createPackage(FqName("kotlin"))
    private val kotlinCoroutinesPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines"))
    private val kotlinCoroutinesJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines.jvm.internal"))
    private val kotlinJvmPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm"))
    private val kotlinJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.internal"))
    private val kotlinJvmFunctionsPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.functions"))
    private val kotlinReflectPackage: IrPackageFragment = createPackage(FqName("kotlin.reflect"))
    private val javaLangPackage: IrPackageFragment = createPackage(FqName("java.lang"))

    private val generateOptimizedCallableReferenceSuperClasses = context.state.generateOptimizedCallableReferenceSuperClasses

    private fun createPackage(fqName: FqName): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(context.state.module, fqName)

    private fun createClass(
        fqName: FqName,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.FINAL,
        classIsInline: Boolean = false,
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol =
        irFactory.buildClass {
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
                "kotlin.jvm" -> kotlinJvmPackage
                "kotlin.reflect" -> kotlinReflectPackage
                "java.lang" -> javaLangPackage
                else -> error("Other packages are not supported yet: $fqName")
            }
            createImplicitParameterDeclarationWithWrappedDescriptor()
            block(this)
        }.symbol

    private val intrinsicsClass: IrClassSymbol = createClass(
        JvmClassName.byInternalName(IrIntrinsicMethods.INTRINSICS_CLASS_NAME).fqNameForTopLevelClassMaybeWithDollars
    ) { klass ->
        klass.addFunction("throwNullPointerException", irBuiltIns.nothingType, isStatic = true).apply {
            addValueParameter("message", irBuiltIns.stringType)
        }
        klass.addFunction("throwTypeCastException", irBuiltIns.nothingType, isStatic = true).apply {
            addValueParameter("message", irBuiltIns.stringType)
        }
        klass.addFunction("throwUnsupportedOperationException", irBuiltIns.nothingType, isStatic = true).apply {
            addValueParameter("message", irBuiltIns.stringType)
        }
        klass.addFunction("throwUninitializedPropertyAccessException", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("propertyName", irBuiltIns.stringType)
        }
        klass.addFunction("throwKotlinNothingValueException", irBuiltIns.nothingType, isStatic = true)
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
        klass.addFunction("checkNotNull", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("object", irBuiltIns.anyNType)
        }
        klass.addFunction("throwNpe", irBuiltIns.unitType, isStatic = true)

        klass.declarations.add(irFactory.buildClass {
            name = Name.identifier("Kotlin")
        }.apply {
            parent = klass
            createImplicitParameterDeclarationWithWrappedDescriptor()
        })
    }

    val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkExpressionValueIsNotNull" }

    val checkNotNullExpressionValue: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkNotNullExpressionValue" }

    val checkNotNull: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkNotNull" }

    val throwNpe: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwNpe" }

    override val throwNullPointerException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwNullPointerException" }

    override val throwNoWhenBranchMatchedException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    override val throwTypeCastException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwTypeCastException" }

    val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwUnsupportedOperationException" }

    override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwUninitializedPropertyAccessException" }

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwKotlinNothingValueException" }

    val intrinsicStringPlus: IrFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "stringPlus" }

    val intrinsicsKotlinClass: IrClassSymbol =
        (intrinsicsClass.owner.declarations.single { it is IrClass && it.name.asString() == "Kotlin" } as IrClass).symbol

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

    private val kDeclarationContainer: IrClassSymbol =
        createClass(StandardNames.FqNames.kDeclarationContainer.toSafe(), ClassKind.INTERFACE, Modality.ABSTRACT)

    val javaLangClass: IrClassSymbol =
        createClass(FqName("java.lang.Class")) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.INVARIANT)
            klass.addFunction("desiredAssertionStatus", irBuiltIns.booleanType)
        }

    private val javaLangDeprecatedWithDeprecatedFlag: IrClassSymbol =
        createClass(FqName("java.lang.Deprecated"), classKind = ClassKind.ANNOTATION_CLASS) { klass ->
            klass.addConstructor { isPrimary = true }
        }

    // This annotations also implies ACC_DEPRECATED flag in generated bytecode
    val javaLangDeprecatedConstructorWithDeprecatedFlag = javaLangDeprecatedWithDeprecatedFlag.constructors.single()

    private val javaLangAssertionError: IrClassSymbol =
        createClass(FqName("java.lang.AssertionError"), classModality = Modality.OPEN) { klass ->
            klass.addConstructor().apply {
                addValueParameter("detailMessage", irBuiltIns.anyNType)
            }
        }

    val assertionErrorConstructor = javaLangAssertionError.constructors.single()

    val continuationClass: IrClassSymbol =
        createClass(StandardNames.CONTINUATION_INTERFACE_FQ_NAME_RELEASE, ClassKind.INTERFACE) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.IN_VARIANCE)
        }

    private val resultClassStub: IrClassSymbol =
        createClass(StandardNames.RESULT_FQ_NAME, classIsInline = true) { klass ->
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
                addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType.makeNullable())
            }
            klass.addFunction(INVOKE_SUSPEND_METHOD_NAME, irBuiltIns.anyNType, Modality.ABSTRACT).apply {
                addValueParameter(SUSPEND_CALL_RESULT_NAME, resultClassStub.typeWith(irBuiltIns.anyNType))
            }
        }

    val suspendFunctionInterface: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.SuspendFunction"), ClassKind.INTERFACE)

    val lambdaClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Lambda"), classModality = Modality.ABSTRACT) { klass ->
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
        }
    }

    val suspendLambdaClass: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.SuspendLambda"), classModality = Modality.ABSTRACT) { klass ->
            addSuspendLambdaInterfaceFunctions(klass)
        }

    val restrictedSuspendLambdaClass: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.RestrictedSuspendLambda"), classModality = Modality.ABSTRACT) { klass ->
            addSuspendLambdaInterfaceFunctions(klass)
        }

    private fun addSuspendLambdaInterfaceFunctions(klass: IrClass) {
        klass.superTypes += suspendFunctionInterface.defaultType
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
            addValueParameter(
                SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
                continuationClass.typeWith(irBuiltIns.anyNType).makeNullable()
            )
        }
        klass.addFunction(INVOKE_SUSPEND_METHOD_NAME, irBuiltIns.anyNType, Modality.ABSTRACT, DescriptorVisibilities.PROTECTED).apply {
            addValueParameter(SUSPEND_CALL_RESULT_NAME, resultClassStub.typeWith(irBuiltIns.anyNType))
        }
        klass.addFunction(SUSPEND_FUNCTION_CREATE_METHOD_NAME, continuationClass.typeWith(irBuiltIns.unitType), Modality.OPEN).apply {
            addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationClass.typeWith(irBuiltIns.nothingType))
        }
        klass.addFunction(SUSPEND_FUNCTION_CREATE_METHOD_NAME, continuationClass.typeWith(irBuiltIns.unitType), Modality.OPEN).apply {
            addValueParameter("value", irBuiltIns.anyNType)
            addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationClass.typeWith(irBuiltIns.nothingType))
        }
    }

    private fun generateCallableReferenceMethods(klass: IrClass) {
        klass.addFunction("getSignature", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getName", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getOwner", kDeclarationContainer.defaultType, Modality.OPEN)
    }

    val functionReference: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.FunctionReference"), classModality = Modality.OPEN) { klass ->
            klass.addConstructor().apply {
                addValueParameter("arity", irBuiltIns.intType)
            }

            klass.addConstructor().apply {
                addValueParameter("arity", irBuiltIns.intType)
                addValueParameter("receiver", irBuiltIns.anyNType)
            }

            klass.addField("receiver", irBuiltIns.anyNType, DescriptorVisibilities.PROTECTED)

            generateCallableReferenceMethods(klass)
        }

    val functionReferenceReceiverField: IrFieldSymbol = functionReference.fieldByName("receiver")
    val functionReferenceGetSignature: IrSimpleFunctionSymbol = functionReference.functionByName("getSignature")
    val functionReferenceGetName: IrSimpleFunctionSymbol = functionReference.functionByName("getName")
    val functionReferenceGetOwner: IrSimpleFunctionSymbol = functionReference.functionByName("getOwner")

    val functionReferenceImpl: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.FunctionReferenceImpl"), classModality = Modality.OPEN) { klass ->
            klass.superTypes = listOf(functionReference.defaultType)

            if (generateOptimizedCallableReferenceSuperClasses) {
                klass.generateCallableReferenceSuperclassConstructors(withArity = true)
            }
        }

    val adaptedFunctionReference: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.AdaptedFunctionReference"), classModality = Modality.OPEN) { klass ->
            klass.superTypes = listOf(irBuiltIns.anyType)
            klass.generateCallableReferenceSuperclassConstructors(withArity = true)
        }

    private fun IrClass.generateCallableReferenceSuperclassConstructors(withArity: Boolean) {
        for (hasBoundReceiver in listOf(false, true)) {
            addConstructor().apply {
                if (withArity) {
                    addValueParameter("arity", irBuiltIns.intType)
                }
                if (hasBoundReceiver) {
                    addValueParameter("receiver", irBuiltIns.anyNType)
                }
                addValueParameter("owner", javaLangClass.starProjectedType)
                addValueParameter("name", irBuiltIns.stringType)
                addValueParameter("signature", irBuiltIns.stringType)
                addValueParameter("flags", irBuiltIns.intType)
            }
        }
    }

    fun getFunction(parameterCount: Int): IrClassSymbol = irBuiltIns.function(parameterCount)

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

    val jvmInlineAnnotation: IrClassSymbol = createClass(JVM_INLINE_ANNOTATION_FQ_NAME, ClassKind.ANNOTATION_CLASS).apply {
        owner.addConstructor {
            isPrimary = true
        }
    }

    private data class PropertyReferenceKey(
        val mutable: Boolean,
        val parameterCount: Int,
        val impl: Boolean
    )

    private val propertyReferenceClassCache = mutableMapOf<PropertyReferenceKey, IrClassSymbol>()

    fun getPropertyReferenceClass(mutable: Boolean, parameterCount: Int, impl: Boolean): IrClassSymbol {
        val key = PropertyReferenceKey(mutable, parameterCount, impl)
        return propertyReferenceClassCache.getOrPut(key) {
            val className = buildString {
                if (mutable) append("Mutable")
                append("PropertyReference")
                append(parameterCount)
                if (impl) append("Impl")
            }

            createClass(
                FqName("kotlin.jvm.internal.$className"),
                classModality = if (impl) Modality.FINAL else Modality.ABSTRACT
            ) { klass ->
                if (impl) {
                    klass.addConstructor().apply {
                        addValueParameter("owner", kDeclarationContainer.defaultType)
                        addValueParameter("name", irBuiltIns.stringType)
                        addValueParameter("string", irBuiltIns.stringType)
                    }

                    if (generateOptimizedCallableReferenceSuperClasses) {
                        klass.generateCallableReferenceSuperclassConstructors(withArity = false)
                    }

                    klass.superTypes += getPropertyReferenceClass(mutable, parameterCount, false).defaultType
                } else {
                    klass.addConstructor()

                    klass.addConstructor().apply {
                        addValueParameter("receiver", irBuiltIns.anyNType)
                    }
                }

                val receiverFieldName = Name.identifier("receiver")
                klass.addProperty() {
                    name = receiverFieldName
                }.apply {
                    backingField = irFactory.buildField {
                        name = receiverFieldName
                        type = irBuiltIns.anyNType
                        visibility = DescriptorVisibilities.PROTECTED
                    }.also { field ->
                        field.parent = klass
                    }
                }

                generateCallableReferenceMethods(klass)

                // To avoid hassle with generic type parameters, we pretend that PropertyReferenceN.get takes and returns `Any?`
                // (similarly with set). This should be enough for the JVM IR backend to generate correct calls and bridges.
                klass.addFunction("get", irBuiltIns.anyNType, Modality.ABSTRACT).apply {
                    for (i in 0 until parameterCount) {
                        addValueParameter("receiver$i", irBuiltIns.anyNType)
                    }
                }

                // invoke redirects to get
                klass.addFunction("invoke", irBuiltIns.anyNType, Modality.FINAL).apply {
                    for (i in 0 until parameterCount) {
                        addValueParameter("receiver$i", irBuiltIns.anyNType)
                    }
                }

                if (mutable) {
                    klass.addFunction("set", irBuiltIns.unitType, Modality.ABSTRACT).apply {
                        for (i in 0 until parameterCount) {
                            addValueParameter("receiver$i", irBuiltIns.anyNType)
                        }
                        addValueParameter("value", irBuiltIns.anyNType)
                    }
                }
            }
        }
    }

    val reflection: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Reflection")) { klass ->
        val javaLangClassType = javaLangClass.starProjectedType
        val kClassType = irBuiltIns.kClassClass.starProjectedType

        klass.addFunction("getOrCreateKotlinPackage", kDeclarationContainer.defaultType, isStatic = true).apply {
            addValueParameter("javaClass", javaLangClassType)
            addValueParameter("moduleName", irBuiltIns.stringType)
        }

        klass.addFunction("getOrCreateKotlinClass", kClassType, isStatic = true).apply {
            addValueParameter("javaClass", javaLangClassType)
        }

        klass.addFunction("getOrCreateKotlinClasses", irBuiltIns.arrayClass.typeWith(kClassType), isStatic = true).apply {
            addValueParameter("javaClasses", irBuiltIns.arrayClass.typeWith(javaLangClassType))
        }

        for (mutable in listOf(false, true)) {
            for (n in 0..2) {
                val functionName = (if (mutable) "mutableProperty" else "property") + n
                klass.addFunction(functionName, irBuiltIns.getKPropertyClass(mutable, n).starProjectedType, isStatic = true).apply {
                    addValueParameter("p", getPropertyReferenceClass(mutable, n, impl = false).defaultType)
                }
            }
        }
    }

    override val functionAdapter: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.FunctionAdapter"), ClassKind.INTERFACE) { klass ->
        klass.addFunction("getFunctionDelegate", irBuiltIns.functionClass.starProjectedType, Modality.ABSTRACT)
    }

    val getOrCreateKotlinPackage: IrSimpleFunctionSymbol =
        reflection.functionByName("getOrCreateKotlinPackage")

    val desiredAssertionStatus: IrSimpleFunctionSymbol by lazy {
        javaLangClass.functionByName("desiredAssertionStatus")
    }

    override val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol =
        irFactory.buildFun {
            name = Name.special("<unsafe-coerce>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }.apply {
            parent = kotlinJvmInternalPackage
            val src = addTypeParameter("T", irBuiltIns.anyNType)
            val dst = addTypeParameter("R", irBuiltIns.anyNType)
            addValueParameter("v", src.defaultType)
            returnType = dst.defaultType
        }.symbol

    private val collectionToArrayClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.CollectionToArray")) { klass ->
        klass.origin = JvmLoweredDeclarationOrigin.TO_ARRAY

        val arrayType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
        val collectionType = irBuiltIns.collectionClass.starProjectedType
        klass.addFunction("toArray", arrayType, isStatic = true).apply {
            origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            addValueParameter("collection", collectionType, JvmLoweredDeclarationOrigin.TO_ARRAY)
        }
        klass.addFunction("toArray", arrayType, isStatic = true).apply {
            origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            addValueParameter("collection", collectionType, JvmLoweredDeclarationOrigin.TO_ARRAY)
            addValueParameter("array", arrayType.makeNullable(), JvmLoweredDeclarationOrigin.TO_ARRAY)
        }
    }

    val nonGenericToArray: IrSimpleFunctionSymbol =
        collectionToArrayClass.functions.single { it.owner.name.asString() == "toArray" && it.owner.valueParameters.size == 1 }

    val genericToArray: IrSimpleFunctionSymbol =
        collectionToArrayClass.functions.single { it.owner.name.asString() == "toArray" && it.owner.valueParameters.size == 2 }

    val kClassJava: IrPropertySymbol =
        irFactory.buildProperty() {
            name = Name.identifier("java")
        }.apply {
            parent = kotlinJvmPackage
            addGetter().apply {
                addExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
                returnType = javaLangClass.starProjectedType
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

            klass.addFunction("toArray", irBuiltIns.primitiveArrayForType.getValue(irType).defaultType)
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

    private val javaLangInteger: IrClassSymbol = createClass(FqName("java.lang.Integer")) { klass ->
        klass.addFunction("compareUnsigned", irBuiltIns.intType, isStatic = true).apply {
            addValueParameter("x", irBuiltIns.intType)
            addValueParameter("y", irBuiltIns.intType)
        }
        klass.addFunction("divideUnsigned", irBuiltIns.intType, isStatic = true).apply {
            addValueParameter("dividend", irBuiltIns.intType)
            addValueParameter("divisor", irBuiltIns.intType)
        }
        klass.addFunction("remainderUnsigned", irBuiltIns.intType, isStatic = true).apply {
            addValueParameter("dividend", irBuiltIns.intType)
            addValueParameter("divisor", irBuiltIns.intType)
        }
        klass.addFunction("toUnsignedString", irBuiltIns.stringType, isStatic = true).apply {
            addValueParameter("i", irBuiltIns.intType)
        }
    }

    val compareUnsignedInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("compareUnsigned")
    val divideUnsignedInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("divideUnsigned")
    val remainderUnsignedInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("remainderUnsigned")
    val toUnsignedStringInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("toUnsignedString")

    private val javaLangLong: IrClassSymbol = createClass(FqName("java.lang.Long")) { klass ->
        klass.addFunction("compareUnsigned", irBuiltIns.intType, isStatic = true).apply {
            addValueParameter("x", irBuiltIns.longType)
            addValueParameter("y", irBuiltIns.longType)
        }
        klass.addFunction("divideUnsigned", irBuiltIns.longType, isStatic = true).apply {
            addValueParameter("dividend", irBuiltIns.longType)
            addValueParameter("divisor", irBuiltIns.longType)
        }
        klass.addFunction("remainderUnsigned", irBuiltIns.longType, isStatic = true).apply {
            addValueParameter("dividend", irBuiltIns.longType)
            addValueParameter("divisor", irBuiltIns.longType)
        }
        klass.addFunction("toUnsignedString", irBuiltIns.stringType, isStatic = true).apply {
            addValueParameter("i", irBuiltIns.longType)
        }
    }

    val compareUnsignedLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("compareUnsigned")
    val divideUnsignedLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("divideUnsigned")
    val remainderUnsignedLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("remainderUnsigned")
    val toUnsignedStringLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("toUnsignedString")

    val systemArraycopy: IrSimpleFunctionSymbol = systemClass.functionByName("arraycopy")

    val signatureStringIntrinsic: IrSimpleFunctionSymbol =
        irFactory.buildFun {
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

    private val javaLangEnum: IrClassSymbol =
        createClass(FqName("java.lang.Enum")) { klass ->
            // The declaration of Enum.valueOf is: `public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name)`
            // But we only need the following type-erased version to generate correct calls.
            klass.addFunction("valueOf", klass.defaultType, isStatic = true).apply {
                addValueParameter("enumType", javaLangClass.starProjectedType)
                addValueParameter("name", irBuiltIns.stringType)
            }
        }

    val enumValueOfFunction: IrSimpleFunctionSymbol =
        javaLangEnum.functionByName("valueOf")

    private val kotlinCoroutinesJvmInternalRunSuspendKt =
        createClass(FqName("kotlin.coroutines.jvm.internal.RunSuspendKt")) { klass ->
            klass.addFunction("runSuspend", irBuiltIns.unitType, isStatic = true).apply {
                addValueParameter(
                    "block",
                    getJvmSuspendFunctionClass(0).typeWith(
                        irBuiltIns.unitType
                    )
                )
            }
        }

    val runSuspendFunction: IrSimpleFunctionSymbol =
        kotlinCoroutinesJvmInternalRunSuspendKt.functionByName("runSuspend")
}

private fun IrClassSymbol.functionByName(name: String): IrSimpleFunctionSymbol =
    functions.single { it.owner.name.asString() == name }

private fun IrClassSymbol.fieldByName(name: String): IrFieldSymbol =
    fields.single { it.owner.name.asString() == name }
