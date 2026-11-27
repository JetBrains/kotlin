/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PropertyName")

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.BackendSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_CALL_RESULT_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_CREATE_METHOD_NAME
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.declarations.v2.irClass
import org.jetbrains.kotlin.ir.builders.declarations.v2.irConstructor
import org.jetbrains.kotlin.ir.builders.declarations.v2.irField
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.builders.declarations.v2.irProperty
import org.jetbrains.kotlin.ir.builders.declarations.v2.irStaticFunction
import org.jetbrains.kotlin.ir.classSymbolOrNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrEnumEntrySymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.lang.invoke.MethodType

class JvmSymbols(
    private val context: JvmBackendContext,
    private val irModule: IrModuleFragment,
) : BackendSymbols(context.irBuiltIns) {
    private val irBuiltIns = context.irBuiltIns
    private val storageManager = LockBasedStorageManager(this::class.java.simpleName)
    private val irFactory = context.irFactory

    private val kotlinPackage: IrPackageFragment = createPackage(FqName("kotlin"))
    private val kotlinCoroutinesPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines"))
    private val kotlinCoroutinesJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines.jvm.internal"))
    private val kotlinJvmPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm"))
    private val kotlinJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.internal"))
    private val kotlinJvmFunctionsPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.functions"))
    private val kotlinEnumsPackage: IrPackageFragment = createPackage(FqName("kotlin.enums"))
    private val kotlinReflectPackage: IrPackageFragment = createPackage(FqName("kotlin.reflect"))
    private val javaLangPackage: IrPackageFragment = createPackage(FqName("java.lang"))
    private val javaLangInvokePackage: IrPackageFragment = createPackage(FqName("java.lang.invoke"))
    private val javaUtilPackage: IrPackageFragment = createPackage(FqName("java.util"))


    private val kotlinInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.internal"))

    /**
     * A special package for functions representing dynamic symbols referenced by the `INVOKEDYNAMIC` instruction — e.g.,
     *  `get(Ljava/lang/String;)Ljava/util/function/Supplier;`
     * in
     * ```
     * INVOKEDYNAMIC get(Ljava/lang/String;)Ljava/util/function/Supplier; [
     *     H_INVOKESTATIC java/lang/invoke/LambdaMetafactory.metafactory(...)Ljava/lang/invoke/CallSite;
     *     ...
     * ]
     * ```
     * Such functions don't exist as methods in the actual bytecode
     * (they are expected to be provided at run-time by the corresponding bootstrap method).
     */
    val kotlinJvmInternalInvokeDynamicPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.internal.invokeDynamic"))

    private fun createPackage(fqName: FqName): IrPackageFragment =
        createEmptyExternalPackageFragment(irModule, fqName)

    private fun createClass(
        fqName: FqName,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.FINAL,
        classIsValue: Boolean = false,
        block: (IrClass) -> Unit = {},
    ): IrClassSymbol =
        irFactory.irClass {
            name = fqName.shortName()
            kind = classKind
            modality = classModality
            isValue = classIsValue
            parent = when (fqName.parent().asString()) {
                "kotlin" -> kotlinPackage
                "kotlin.coroutines" -> kotlinCoroutinesPackage
                "kotlin.coroutines.jvm.internal" -> kotlinCoroutinesJvmInternalPackage
                "kotlin.enums" -> kotlinEnumsPackage
                "kotlin.jvm.internal" -> kotlinJvmInternalPackage
                "kotlin.jvm.functions" -> kotlinJvmFunctionsPackage
                "kotlin.jvm" -> kotlinJvmPackage
                "kotlin.reflect" -> kotlinReflectPackage
                "java.lang" -> javaLangPackage
                "java.lang.invoke" -> javaLangInvokePackage
                "java.util" -> javaUtilPackage
                "kotlin.internal" -> kotlinInternalPackage
                else -> error("Other packages are not supported yet: $fqName")
            }
            thisReceiver()
        }.apply {
            block(this)
        }.symbol

    private val intrinsicsClass: IrClassSymbol = createClass(
        JvmClassName.byInternalName(INTRINSICS_CLASS_NAME).fqNameForTopLevelClassMaybeWithDollars
    ) { klass ->
        klass.irStaticFunction {
            name = Name.identifier("throwNullPointerException")
            returnType = irBuiltIns.nothingType
            valueParameter("message", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("throwTypeCastException")
            returnType = irBuiltIns.nothingType
            valueParameter("message", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("throwIllegalAccessException")
            returnType = irBuiltIns.nothingType
            valueParameter("message", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("throwUnsupportedOperationException")
            returnType = irBuiltIns.nothingType
            valueParameter("message", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("throwUninitializedPropertyAccessException")
            returnType = irBuiltIns.unitType
            valueParameter("propertyName", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("throwKotlinNothingValueException")
            returnType = irBuiltIns.nothingType
        }
        klass.irStaticFunction {
            name = Name.identifier("checkExpressionValueIsNotNull")
            returnType = irBuiltIns.unitType
            valueParameter("value", irBuiltIns.anyNType)
            valueParameter("expression", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("checkNotNullExpressionValue")
            returnType = irBuiltIns.unitType
            valueParameter("value", irBuiltIns.anyNType)
            valueParameter("expression", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("stringPlus")
            returnType = irBuiltIns.stringType
            valueParameter("self", irBuiltIns.stringType.makeNullable())
            valueParameter("other", irBuiltIns.anyNType)
        }
        klass.irStaticFunction {
            name = Name.identifier("checkNotNull")
            returnType = irBuiltIns.unitType
            valueParameter("object", irBuiltIns.anyNType)
        }
        klass.irStaticFunction {
            name = Name.identifier("checkNotNull")
            returnType = irBuiltIns.unitType
            valueParameter("object", irBuiltIns.anyNType)
            valueParameter("message", irBuiltIns.stringType)
        }
        klass.irStaticFunction {
            name = Name.identifier("throwNpe")
            returnType = irBuiltIns.unitType
        }

        klass.declarations.add(irFactory.irClass {
            name = Name.identifier("Kotlin")
            parent = klass
            thisReceiver()
        })
    }

    val generatedCodeMarkersInCoroutinesClass: IrClassSymbol = createClass(
        FqName("kotlin.coroutines.jvm.internal.GeneratedCodeMarkers")
    ) { klass ->
        klass.irStaticFunction {
            name = Name.identifier("checkContinuation")
            returnType = irBuiltIns.unitType
            isInline = true
        }
        klass.irStaticFunction {
            name = Name.identifier("lambdaArgumentsUnspilling")
            returnType = irBuiltIns.unitType
            isInline = true
        }
        klass.irStaticFunction {
            name = Name.identifier("tableswitch")
            returnType = irBuiltIns.unitType
            isInline = true
        }
        klass.irStaticFunction {
            name = Name.identifier("checkResult")
            returnType = irBuiltIns.unitType
            isInline = true
        }
        klass.irStaticFunction {
            name = Name.identifier("checkCOROUTINE_SUSPENDED")
            returnType = irBuiltIns.unitType
            isInline = true
        }
        klass.irStaticFunction {
            name = Name.identifier("unreachable")
            returnType = irBuiltIns.unitType
            isInline = true
        }
    }

    val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkExpressionValueIsNotNull" }

    val checkNotNullExpressionValue: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "checkNotNullExpressionValue" }

    val checkNotNull: IrSimpleFunctionSymbol =
        intrinsicsClass.owner.functions.single {
            it.name.asString() == "checkNotNull" && it.hasShape(regularParameters = 1)
        }.symbol

    val checkNotNullWithMessage: IrSimpleFunctionSymbol =
        intrinsicsClass.owner.functions.single {
            it.name.asString() == "checkNotNull" && it.hasShape(regularParameters = 2)
        }.symbol

    val throwNpe: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwNpe" }

    override val throwNullPointerException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwNullPointerException" }

    override val throwTypeCastException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwTypeCastException" }

    val throwIllegalAccessException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwIllegalAccessException" }

    override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwUnsupportedOperationException" }

    override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwUninitializedPropertyAccessException" }

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwKotlinNothingValueException" }

    val intrinsicsKotlinClass: IrClassSymbol =
        (intrinsicsClass.owner.declarations.single { it is IrClass && it.name.asString() == "Kotlin" } as IrClass).symbol

    override val stringBuilder: IrClassSymbol = createClass(FqName("java.lang.StringBuilder")) { klass ->
        klass.irConstructor()
        klass.irFunction {
            name = Name.identifier("toString")
            returnType = irBuiltIns.stringType
            overriddenSymbols = [irBuiltIns.anyClass.functionByName("toString")]
        }

        val appendTypes = with(irBuiltIns) {
            listOf(
                anyNType,
                stringType.makeNullable(),
                booleanType, charType, intType, longType, floatType, doubleType
            )
        }
        for (type in appendTypes) {
            klass.irFunction {
                name = Name.identifier("append")
                returnType = klass.defaultType
                valueParameter("value", type)
            }
        }
    }

    val enumEntries: IrClassSymbol = createClass(FqName("kotlin.enums.EnumEntries"), ClassKind.INTERFACE) { klass ->
        // Actually it is E : Enum<E>, but doesn't seem to have any effect yet
        klass.addTypeParameter("E", irBuiltIns.anyNType)
    }

    private val enumEntriesKt: IrClassSymbol = createClass(FqName("kotlin.enums.EnumEntriesKt")) { klass ->
        klass.irStaticFunction {
            name = Name.identifier("enumEntries")
            returnType = enumEntries.defaultType
            val e = typeParameter("E", irBuiltIns.enumClass.defaultType)
            valueParameter("entries", irBuiltIns.arrayClass.typeWith(e.defaultType))
        }
    }

    val createEnumEntries: IrSimpleFunctionSymbol = enumEntriesKt.functions.single { it.owner.name.asString() == "enumEntries" }

    override val defaultConstructorMarker: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.DefaultConstructorMarker"))

    override val syntheticConstructorMarker: IrClassSymbol
        get() = error("not implemented")

    override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
        get() = error("not implemented")

    override val getContinuation: IrSimpleFunctionSymbol
        get() = error("not implemented")

    override val coroutineContextGetter: IrSimpleFunctionSymbol
        get() = error("not implemented")

    override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
        get() = error("not implemented")

    override val coroutineGetContext: IrSimpleFunctionSymbol
        get() = error("not implemented")

    override val returnIfSuspended: IrSimpleFunctionSymbol
        get() = error("not implemented")

    private val kDeclarationContainer: IrClassSymbol =
        createClass(StandardNames.FqNames.kDeclarationContainer.toSafe(), ClassKind.INTERFACE, Modality.ABSTRACT)

    val javaLangClass: IrClassSymbol =
        createClass(FqName("java.lang.Class")) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.INVARIANT)
            klass.irFunction {
                name = Name.identifier("desiredAssertionStatus")
                returnType = irBuiltIns.booleanType
            }
        }

    val javaLangDeprecatedWithDeprecatedFlag: IrClassSymbol =
        createClass(FqName("java.lang.Deprecated"), classKind = ClassKind.ANNOTATION_CLASS) { klass ->
            klass.irConstructor { isPrimary = true }
        }

    // This annotation also implies ACC_DEPRECATED flag in generated bytecode
    val javaLangDeprecatedConstructorWithDeprecatedFlag = javaLangDeprecatedWithDeprecatedFlag.constructors.single()

    private val javaLangAssertionError: IrClassSymbol =
        createClass(FqName("java.lang.AssertionError"), classModality = Modality.OPEN) { klass ->
            klass.irConstructor {
                valueParameter("detailMessage", irBuiltIns.anyNType)
            }
        }

    val assertionErrorConstructor = javaLangAssertionError.constructors.single()

    private val javaLangNoSuchFieldError: IrClassSymbol =
        createClass(FqName("java.lang.NoSuchFieldError"), classModality = Modality.OPEN) {}

    val noSuchFieldErrorType = javaLangNoSuchFieldError.defaultType

    override val continuationClass: IrClassSymbol =
        createClass(StandardNames.CONTINUATION_INTERFACE_FQ_NAME, ClassKind.INTERFACE) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.IN_VARIANCE)
        }

    private val resultClassStub: IrClassSymbol =
        createClass(StandardNames.RESULT_FQ_NAME, classIsValue = true) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.OUT_VARIANCE)
            klass.valueClassRepresentation = InlineClassRepresentation(Name.identifier("value"), irBuiltIns.anyNType as IrSimpleType)
        }

    val resultOfAnyType: IrType = resultClassStub.typeWith(irBuiltIns.anyNType)

    val continuationImplClass: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.ContinuationImpl"), classModality = Modality.ABSTRACT) { klass ->
            val continuationType = continuationClass.typeWith(irBuiltIns.anyNType)
            klass.superTypes += continuationType
            klass.irConstructor {
                valueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType.makeNullable())
            }
            klass.irFunction {
                name = Name.identifier(INVOKE_SUSPEND_METHOD_NAME)
                returnType = irBuiltIns.anyNType
                modality = Modality.ABSTRACT
                valueParameter(SUSPEND_CALL_RESULT_NAME, resultOfAnyType)
            }
        }

    val suspendFunctionInterface: IrClassSymbol =
        createClass(FqName("kotlin.coroutines.jvm.internal.SuspendFunction"), ClassKind.INTERFACE)

    val lambdaClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Lambda"), classModality = Modality.ABSTRACT) { klass ->
        klass.irConstructor {
            valueParameter("arity", irBuiltIns.intType)
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
        klass.irConstructor {
            valueParameter("arity", irBuiltIns.intType)
            valueParameter(
                SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
                continuationClass.typeWith(irBuiltIns.anyNType).makeNullable()
            )
        }
        klass.irFunction {
            name = Name.identifier(INVOKE_SUSPEND_METHOD_NAME)
            returnType = irBuiltIns.anyNType
            modality = Modality.ABSTRACT
            visibility = DescriptorVisibilities.PROTECTED
            valueParameter(SUSPEND_CALL_RESULT_NAME, resultOfAnyType)
        }
        klass.irFunction {
            name = Name.identifier(SUSPEND_FUNCTION_CREATE_METHOD_NAME)
            returnType = continuationClass.typeWith(irBuiltIns.unitType)
            modality = Modality.OPEN
            valueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationClass.typeWith(irBuiltIns.nothingType))
        }
        klass.irFunction {
            name = Name.identifier(SUSPEND_FUNCTION_CREATE_METHOD_NAME)
            returnType = continuationClass.typeWith(irBuiltIns.unitType)
            modality = Modality.OPEN
            valueParameter("value", irBuiltIns.anyNType)
            valueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationClass.typeWith(irBuiltIns.nothingType))
        }
    }

    private fun generateCallableReferenceMethods(klass: IrClass) {
        klass.irFunction {
            name = Name.identifier("getSignature")
            returnType = irBuiltIns.stringType
            modality = Modality.OPEN
        }
        klass.irFunction {
            name = Name.identifier("getName")
            returnType = irBuiltIns.stringType
            modality = Modality.OPEN
        }
        klass.irFunction {
            name = Name.identifier("getOwner")
            returnType = kDeclarationContainer.defaultType
            modality = Modality.OPEN
        }
    }

    val functionReference: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.FunctionReference"), classModality = Modality.OPEN) { klass ->
            klass.irConstructor {
                valueParameter("arity", irBuiltIns.intType)
            }

            klass.irConstructor {
                valueParameter("arity", irBuiltIns.intType)
                valueParameter("receiver", irBuiltIns.anyNType)
            }

            klass.irField {
                name = Name.identifier("receiver")
                type = irBuiltIns.anyNType
                visibility = DescriptorVisibilities.PROTECTED
            }

            generateCallableReferenceMethods(klass)
        }

    val functionReferenceImpl: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.FunctionReferenceImpl"), classModality = Modality.OPEN) { klass ->
            klass.superTypes = listOf(functionReference.defaultType)
            klass.addFunctionReferenceSuperclassMembers()
        }

    val adaptedFunctionReference: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.AdaptedFunctionReference"), classModality = Modality.OPEN) { klass ->
            klass.superTypes = listOf(irBuiltIns.anyType)
            klass.addFunctionReferenceSuperclassMembers()
        }

    private fun IrClass.addFunctionReferenceSuperclassMembers() {
        addBoundContextArgumentsField()
        for (withContextArguments in listOf(false, true)) {
            for (withReceiver in listOf(false, true)) {
                addCallableReferenceConstructor(withArity = true, withContextArguments = withContextArguments, withReceiver = withReceiver)
            }
        }
    }

    private fun IrClass.addBoundContextArgumentsField() {
        irField {
            name = BOUND_CONTEXT_ARGUMENTS_FIELD_NAME
            type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
            visibility = DescriptorVisibilities.PROTECTED
        }
    }

    private fun IrClass.addCallableReferenceConstructor(
        withArity: Boolean,
        withContextArguments: Boolean,
        withReceiver: Boolean,
    ) {
        irConstructor {
            if (withArity) {
                valueParameter("arity", irBuiltIns.intType)
            }
            if (withContextArguments) {
                valueParameter(CONTEXT_ARGUMENTS_PARAMETER_NAME, irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType))
            }
            if (withReceiver) {
                valueParameter(RECEIVER_PARAMETER_NAME, irBuiltIns.anyNType)
            }
            valueParameter("owner", javaLangClass.starProjectedType)
            valueParameter("name", irBuiltIns.stringType)
            valueParameter("signature", irBuiltIns.stringType)
            valueParameter("flags", irBuiltIns.intType)
        }
    }

    val funInterfaceConstructorReferenceClass =
        createClass(FqName("kotlin.jvm.internal.FunInterfaceConstructorReference"), classModality = Modality.OPEN) { irClass ->
            irClass.superTypes = listOf(irBuiltIns.anyType)
            irClass.irConstructor {
                valueParameter("funInterface", javaLangClass.starProjectedType)
            }
        }

    fun getFunction(parameterCount: Int): IrClassSymbol = irBuiltIns.functionN(parameterCount).symbol

    private val jvmFunctionClasses = storageManager.createMemoizedFunction { n: Int ->
        createFunctionClass(n, false)
    }

    private fun createFunctionClass(n: Int, isSuspend: Boolean): IrClassSymbol =
        createClass(FqName("kotlin.jvm.functions.Function${n + if (isSuspend) 1 else 0}"), ClassKind.INTERFACE) { klass ->
            for (i in 1..n) {
                klass.addTypeParameter("P$i", irBuiltIns.anyNType, Variance.IN_VARIANCE)
            }
            val returnType = klass.addTypeParameter("R", irBuiltIns.anyNType, Variance.OUT_VARIANCE)

            klass.irFunction {
                name = Name.identifier("invoke")
                this.returnType = returnType.defaultType
                modality = Modality.ABSTRACT
                this.isSuspend = isSuspend
                for (i in 1..n) {
                    valueParameter("p$i", klass.typeParameters[i - 1].defaultType)
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

        klass.irFunction {
            name = Name.identifier("invoke")
            this.returnType = returnType.defaultType
            modality = Modality.ABSTRACT
            valueParameter {
                name = Name.identifier("args")
                type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
                origin = IrDeclarationOrigin.DEFINED
                varargElementType = irBuiltIns.anyNType
            }
        }
    }

    val jvmInlineAnnotation: IrClassSymbol = createClass(JVM_INLINE_ANNOTATION_FQ_NAME, ClassKind.ANNOTATION_CLASS).apply {
        owner.irConstructor {
            isPrimary = true
        }
    }

    val jvmExposeBoxedAnnotation: IrClassSymbol = createClass(JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME, ClassKind.ANNOTATION_CLASS).apply {
        owner.irConstructor {
            isPrimary = true
            valueParameter("jvmName", irBuiltIns.stringType)
        }
    }

    val boxingConstructorMarkerClass: IrClassSymbol = createClass(JvmStandardClassIds.JVM_EXPOSE_BOXED_NON_EXPOSED_CONSTRUCTOR_MARKER_FQ_NAME)

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
                    klass.addBoundContextArgumentsField()
                    klass.addCallableReferenceConstructor(withArity = false, withContextArguments = false, withReceiver = false)
                    if (parameterCount <= 1) {
                        klass.addCallableReferenceConstructor(withArity = false, withContextArguments = false, withReceiver = true)
                        klass.addCallableReferenceConstructor(withArity = false, withContextArguments = true, withReceiver = false)
                    }
                    if (parameterCount == 0) {
                        klass.addCallableReferenceConstructor(withArity = false, withContextArguments = true, withReceiver = true)
                    }

                    klass.superTypes += getPropertyReferenceClass(mutable, parameterCount, false).defaultType
                } else {
                    klass.irConstructor()

                    klass.irConstructor {
                        valueParameter("receiver", irBuiltIns.anyNType)
                    }
                }

                val receiverFieldName = Name.identifier("receiver")
                klass.irProperty {
                    name = receiverFieldName
                    backingField {
                        origin = IrDeclarationOrigin.DEFINED
                        type = irBuiltIns.anyNType
                        visibility = DescriptorVisibilities.PROTECTED
                    }
                }

                generateCallableReferenceMethods(klass)

                // To avoid hassle with generic type parameters, we pretend that PropertyReferenceN.get takes and returns `Any?`
                // (similarly with set). This should be enough for the JVM IR backend to generate correct calls and bridges.
                klass.irFunction {
                    name = Name.identifier("get")
                    returnType = irBuiltIns.anyNType
                    modality = Modality.ABSTRACT
                    for (i in 0 until parameterCount) {
                        valueParameter("receiver$i", irBuiltIns.anyNType)
                    }
                }

                // invoke redirects to get
                klass.irFunction {
                    name = Name.identifier("invoke")
                    returnType = irBuiltIns.anyNType
                    modality = Modality.FINAL
                    for (i in 0 until parameterCount) {
                        valueParameter("receiver$i", irBuiltIns.anyNType)
                    }
                }

                if (mutable) {
                    klass.irFunction {
                        name = Name.identifier("set")
                        returnType = irBuiltIns.unitType
                        modality = Modality.ABSTRACT
                        for (i in 0 until parameterCount) {
                            valueParameter("receiver$i", irBuiltIns.anyNType)
                        }
                        valueParameter("value", irBuiltIns.anyNType)
                    }
                }
            }
        }
    }

    val javaLangReflectSymbols: JvmReflectSymbols by lazy {
        JvmReflectSymbols(context, irModule)
    }

    override val functionAdapter: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.FunctionAdapter"), ClassKind.INTERFACE) { klass ->
        klass.irFunction {
            name = Name.identifier("getFunctionDelegate")
            returnType = irBuiltIns.functionClass.starProjectedType
            modality = Modality.ABSTRACT
        }
    }

    val desiredAssertionStatus: IrSimpleFunctionSymbol by lazy {
        javaLangClass.functionByName("desiredAssertionStatus")
    }

    override val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<unsafe-coerce>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            val src = typeParameter("T", irBuiltIns.anyNType)
            val dst = typeParameter("R", irBuiltIns.anyNType)
            valueParameter("v", src.defaultType)
            returnType = dst.defaultType
        }.symbol

    private val progressionUtilClasses by lazy(LazyThreadSafetyMode.PUBLICATION) {
        listOf(
            "kotlin.internal.ProgressionUtilKt" to listOf(irBuiltIns.intClass, irBuiltIns.longClass),
            "kotlin.internal.UProgressionUtilKt" to listOfNotNull(irBuiltIns.uintClass, irBuiltIns.ulongClass)
        ).map { [fqn, types] ->
            createClass(FqName(fqn)) { klass ->
                for (type in types) {
                    klass.irStaticFunction {
                        name = Name.identifier("getProgressionLastElement")
                        returnType = type.owner.defaultType
                        for (paramName in arrayOf("s", "e")) {
                            valueParameter(paramName, type.owner.defaultType)
                        }
                        valueParameter(
                            "st",
                            when (type) {
                                irBuiltIns.uintClass -> irBuiltIns.intClass.defaultType
                                irBuiltIns.ulongClass -> irBuiltIns.longClass.defaultType
                                else -> type.owner.defaultType
                            }
                        )
                    }
                }
            }
        }
    }

    override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        progressionUtilClasses.flatMap { klass ->
            klass.functions.filter {
                it.owner.name.identifier == "getProgressionLastElement"
            }.map {
                it.owner.returnType.classifierOrFail to it
            }
        }.toMap()
    }


    val arrayOfAnyType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyType)
    val arrayOfAnyNType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)

    inner class SerializedLambdaClass
    @Deprecated("Should not be used outside of JvmSymbols") internal constructor() {
        val symbol = createClass(FqName("java.lang.invoke.SerializedLambda"))
        val irClass = symbol.owner
        val irType = irClass.defaultType

        val getImplMethodName = irClass.irFunction {
            name = Name.identifier("getImplMethodName")
            returnType = irBuiltIns.stringType
        }
        val getImplMethodKind = irClass.irFunction {
            name = Name.identifier("getImplMethodKind")
            returnType = irBuiltIns.intType
        }
        val getImplClass = irClass.irFunction {
            name = Name.identifier("getImplClass")
            returnType = irBuiltIns.stringType
        }
        val getImplMethodSignature = irClass.irFunction {
            name = Name.identifier("getImplMethodSignature")
            returnType = irBuiltIns.stringType
        }

        val getFunctionalInterfaceClass = irClass.irFunction {
            name = Name.identifier("getFunctionalInterfaceClass")
            returnType = irBuiltIns.stringType
        }
        val getFunctionalInterfaceMethodName = irClass.irFunction {
            name = Name.identifier("getFunctionalInterfaceMethodName")
            returnType = irBuiltIns.stringType
        }
        val getFunctionalInterfaceMethodSignature = irClass.irFunction {
            name = Name.identifier("getFunctionalInterfaceMethodSignature")
            returnType = irBuiltIns.stringType
        }

        val getCapturedArg = irClass.irFunction {
            name = Name.identifier("getCapturedArg")
            returnType = irBuiltIns.anyNType
            valueParameter(Name.identifier("i"), irBuiltIns.intType)
        }
    }

    @Suppress("DEPRECATION")
    val serializedLambda = SerializedLambdaClass()

    val illegalArgumentException = createClass(FqName("java.lang.IllegalArgumentException")) { irClass ->
        irClass.irConstructor {
            valueParameter("message", irBuiltIns.stringType)
        }
    }
    val illegalArgumentExceptionCtorString = illegalArgumentException.constructors.single()

    val jvmMethodType: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<jvm-method-type>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            returnType = irBuiltIns.anyType
            parent = kotlinJvmInternalPackage
            valueParameter("descriptor", irBuiltIns.stringType)
        }.symbol

    val jvmMethodHandle: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<jvm-method-handle>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            returnType = irBuiltIns.anyType
            parent = kotlinJvmInternalPackage
            valueParameter("tag", irBuiltIns.intType)
            valueParameter("owner", irBuiltIns.stringType)
            valueParameter("name", irBuiltIns.stringType)
            valueParameter("descriptor", irBuiltIns.stringType)
            valueParameter("isInterface", irBuiltIns.booleanType)
        }.symbol

    /**
     * An intrinsic to represent `INVOKEDYNAMIC` calls in IR.
     *
     * ```kotlin
     * fun <T> `<jvm-indy>`(
     *     dynamicCall: T,
     *     bootstrapMethodHandle: Any,
     *     vararg bootstrapMethodArgs: Any
     * ): T
     * ```
     * Bootstrap method handle is represented as a `<jvm-method-handle>` call.
     */
    val jvmIndyIntrinsic: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<jvm-indy>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            val t = typeParameter("T", irBuiltIns.anyNType)
            valueParameter("dynamicCall", t.defaultType)
            valueParameter("bootstrapMethodHandle", irBuiltIns.anyType)
            valueParameter {
                name = Name.identifier("bootstrapMethodArguments")
                type = arrayOfAnyType
                varargElementType = irBuiltIns.anyType
            }
            returnType = t.defaultType
        }.symbol

    /**
     * An intrinsic used to represent [MethodType] objects in bootstrap method arguments (see [jvmIndyIntrinsic] above).
     * The value argument is a raw function reference to the corresponding method (e.g., `java.lang.function.Supplier#get`).
     * The resulting method type is unsubstituted.
     * ```kotlin
     * fun `<jvm-original-method-type>`(method: Any): Any
     * ```
     */
    val jvmOriginalMethodTypeIntrinsic: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<jvm-original-method-type>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            valueParameter("method", irBuiltIns.anyType)
            returnType = irBuiltIns.anyType
        }.symbol

    val jvmDebuggerInvokeSpecialIntrinsic: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<jvm-debugger-invokespecial>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            dispatchReceiver {
                type = irBuiltIns.anyNType
            }
            valueParameter("owner", irBuiltIns.stringType)
            valueParameter("name", irBuiltIns.stringType)
            valueParameter("descriptor", irBuiltIns.stringType)
            valueParameter("isInterface", irBuiltIns.booleanType)
            valueParameter("args", irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType))
            returnType = irBuiltIns.anyNType
        }.symbol

    val getClassByDescriptor: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<get-class>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            valueParameter("descriptor", irBuiltIns.stringType)
            returnType = javaLangClass.defaultType
        }.symbol

    val handleResultOfReflectiveAccess: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<coerce-result-of-reflective-access>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            val coerceTo = typeParameter("T", irBuiltIns.anyNType)
            valueParameter("value", irBuiltIns.anyNType)
            returnType = coerceTo.defaultType
        }.symbol

    private val collectionToArrayClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.CollectionToArray")) { klass ->
        klass.origin = JvmLoweredDeclarationOrigin.TO_ARRAY

        val arrayType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
        val collectionType = irBuiltIns.collectionClass.starProjectedType
        klass.irStaticFunction {
            name = Name.identifier("toArray")
            returnType = arrayType
            origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            valueParameter {
                name = Name.identifier("collection")
                type = collectionType
                origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            }
        }
        klass.irStaticFunction {
            name = Name.identifier("toArray")
            returnType = arrayType
            origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            valueParameter {
                name = Name.identifier("collection")
                type = collectionType
                origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            }
            valueParameter {
                name = Name.identifier("array")
                type = arrayType.makeNullable()
                origin = JvmLoweredDeclarationOrigin.TO_ARRAY
            }
        }
    }

    val nonGenericToArray: IrSimpleFunctionSymbol =
        collectionToArrayClass.functions.single {
            it.owner.name.asString() == "toArray" && it.owner.hasShape(regularParameters = 1)
        }

    val genericToArray: IrSimpleFunctionSymbol =
        collectionToArrayClass.functions.single {
            it.owner.name.asString() == "toArray" && it.owner.hasShape(regularParameters = 2)
        }

    val jvmName: IrClassSymbol = createClass(FqName("kotlin.jvm.JvmName"), ClassKind.ANNOTATION_CLASS) { klass ->
        klass.irConstructor {
            valueParameter("name", irBuiltIns.stringType)
        }
    }

    val kClassJava: IrPropertySymbol =
        irFactory.irProperty {
            name = Name.identifier("java")
            parent = createClass(FqName("kotlin.jvm.JvmClassMappingKt")).owner
            getter {
                returnType = javaLangClass.starProjectedType
                extensionReceiver {
                    name = "receiver".synthesizedName
                    type = irBuiltIns.kClassClass.starProjectedType
                }
            }
        }.apply {
            getter!!.annotations = listOf(
                IrAnnotationImpl.fromSymbolOwner(jvmName.typeWith(), jvmName.constructors.single()).apply {
                    arguments[0] = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.stringType, "getJavaClass")
                }
            )
        }.symbol

    val kClassJavaPropertyGetter: IrSimpleFunction =
        kClassJava.owner.getter!!

    val spreadBuilder: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.SpreadBuilder")) { klass ->
        klass.irConstructor {
            valueParameter("size", irBuiltIns.intType)
        }

        klass.irFunction {
            name = Name.identifier("addSpread")
            returnType = irBuiltIns.unitType
            valueParameter("container", irBuiltIns.anyNType)
        }

        klass.irFunction {
            name = Name.identifier("add")
            returnType = irBuiltIns.unitType
            valueParameter("element", irBuiltIns.anyNType)
        }

        klass.irFunction {
            name = Name.identifier("size")
            returnType = irBuiltIns.intType
        }

        klass.irFunction {
            name = Name.identifier("toArray")
            returnType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
            valueParameter("a", irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType))
        }
    }

    val primitiveSpreadBuilders: Map<IrType, IrClassSymbol> = irBuiltIns.primitiveIrTypes.associateWith { irType ->
        val name = irType.classOrNull!!.owner.name
        createClass(FqName("kotlin.jvm.internal.${name}SpreadBuilder")) { klass ->
            klass.irConstructor {
                valueParameter("size", irBuiltIns.intType)
            }

            klass.irFunction {
                this.name = Name.identifier("addSpread")
                returnType = irBuiltIns.unitType
                // This is really a generic method in the superclass (PrimitiveSpreadBuilder).
                // That is why the argument type is Object rather than the correct
                // primitive array type.
                valueParameter("container", irBuiltIns.anyNType)
            }

            klass.irFunction {
                this.name = Name.identifier("add")
                returnType = irBuiltIns.unitType
                valueParameter("element", irType)
            }

            klass.irFunction {
                this.name = Name.identifier("toArray")
                returnType = irBuiltIns.primitiveArrayForType.getValue(irType).defaultType
            }
        }
    }

    private val arraysCopyOfFunctions = HashMap<IrClassifierSymbol, IrSimpleFunction>()

    private fun IrClass.addArraysCopyOfFunction(arrayType: IrSimpleType) {
        irStaticFunction {
            name = Name.identifier("copyOf")
            returnType = arrayType
            valueParameter("original", arrayType)
            valueParameter("newLength", irBuiltIns.intType)
        }.also {
            arraysCopyOfFunctions[arrayType.classifierOrFail] = it
        }
    }

    private fun IrClass.addArraysEqualsFunction(arrayType: IrSimpleType) {
        irStaticFunction {
            name = Name.identifier("equals")
            returnType = irBuiltIns.booleanType
            valueParameter("a", arrayType)
            valueParameter("b", arrayType)
        }
    }

    val arraysClass: IrClassSymbol =
        createClass(FqName("java.util.Arrays")) { irClass ->
            for (type in listOf(
                irBuiltIns.booleanArray.defaultType,
                irBuiltIns.byteArray.defaultType,
                irBuiltIns.charArray.defaultType,
                irBuiltIns.shortArray.defaultType,
                irBuiltIns.intArray.defaultType,
                irBuiltIns.longArray.defaultType,
                irBuiltIns.floatArray.defaultType,
                irBuiltIns.doubleArray.defaultType,
                arrayOfAnyNType
            )) {
                irClass.addArraysCopyOfFunction(type)
                irClass.addArraysEqualsFunction(type)
            }
        }

    fun getArraysCopyOfFunction(arrayType: IrSimpleType): IrSimpleFunctionSymbol {
        val classifier = arrayType.classifier
        val copyOf = arraysCopyOfFunctions[classifier]
        if (copyOf != null)
            return copyOf.symbol
        else
            throw AssertionError("Array type expected: ${arrayType.render()}")
    }

    private val javaLangInteger: IrClassSymbol = createJavaPrimitiveClassWithUnsignedUtils(FqName("java.lang.Integer"), irBuiltIns.intType)

    val compareUnsignedInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("compareUnsigned")
    val divideUnsignedInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("divideUnsigned")
    val remainderUnsignedInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("remainderUnsigned")
    val toUnsignedStringInt: IrSimpleFunctionSymbol = javaLangInteger.functionByName("toUnsignedString")

    private val javaLangLong: IrClassSymbol = createJavaPrimitiveClassWithUnsignedUtils(FqName("java.lang.Long"), irBuiltIns.longType)

    val compareUnsignedLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("compareUnsigned")
    val divideUnsignedLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("divideUnsigned")
    val remainderUnsignedLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("remainderUnsigned")
    val toUnsignedStringLong: IrSimpleFunctionSymbol = javaLangLong.functionByName("toUnsignedString")

    private val unsignedArrayClasses: List<IrClassSymbol> =
        StandardClassIds.elementTypeByUnsignedArrayType.keys.mapNotNull { it.classSymbolOrNull() }

    // List -> ListStaticMembers
    val mappedCollectionStaticMembers: Map<ClassId, IrClassSymbol> =
        [
            StandardClassIds.List,
            StandardClassIds.MutableList,
            StandardClassIds.Set,
            StandardClassIds.MutableSet
        ].mapNotNull { collectionClassId ->
            val staticMembersClassName = Name.identifier("${collectionClassId.shortClassName}StaticMembers")
            ClassId(kotlinJvmInternalPackage.packageFqName, staticMembersClassName).classSymbolOrNull()
                ?.let { collectionClassId to it }
        }.toMap()

    private val unsignedArraySizeGetters: Map<IrClassSymbol, IrSimpleFunctionSymbol> =
        unsignedArrayClasses.mapNotNull { arrayClass ->
            arrayClass.owner.getPropertyGetter("size")?.let { arrayClass to it }
        }.toMap()

    private val unsignedArrayElementGetters: Map<IrClassSymbol, IrSimpleFunctionSymbol> =
        unsignedArrayClasses.mapNotNull { arrayClass ->
            arrayClass.owner.functions.singleOrNull {
                it.name == OperatorNameConventions.GET &&
                        it.hasShape(
                            dispatchReceiver = true,
                            regularParameters = 1,
                            parameterTypes = listOf(null, irBuiltIns.intType)
                        )
            }?.let { arrayClass to it.symbol }
        }.toMap()

    override fun arraySizePropertyGetter(arrayType: IrType): IrSimpleFunction {
        if (arrayType.isUnsignedArray()) {
            return unsignedArraySizeGetters[arrayType.getClass()!!.symbol]?.owner
                ?: error("size getter symbol not found for ${arrayType.getClass()}")
        }
        return super.arraySizePropertyGetter(arrayType)
    }

    override fun arrayElementGetter(arrayType: IrType, intType: IrType): IrSimpleFunction {
        if (arrayType.isUnsignedArray()) {
            return unsignedArrayElementGetters[arrayType.getClass()!!.symbol]?.owner
                ?: error("element getter symbol not found for ${arrayType.getClass()}")
        }
        return super.arrayElementGetter(arrayType, intType)
    }

    val intPostfixIncrDecr = createIncrDecrFun("<int-postfix-incr-decr>")
    val intPrefixIncrDecr = createIncrDecrFun("<int-prefix-incr-decr>")

    private fun createIncrDecrFun(intrinsicName: String): IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special(intrinsicName)
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            valueParameter("value", irBuiltIns.intType)
            valueParameter("delta", irBuiltIns.intType)
            returnType = irBuiltIns.intType
        }.symbol

    private fun createJavaPrimitiveClassWithUnsignedUtils(fqName: FqName, type: IrType): IrClassSymbol = createClass(fqName) { klass ->
        klass.irStaticFunction {
            name = Name.identifier("compareUnsigned")
            returnType = irBuiltIns.intType
            valueParameter("x", type)
            valueParameter("y", type)
        }
        klass.irStaticFunction {
            name = Name.identifier("divideUnsigned")
            returnType = type
            valueParameter("dividend", type)
            valueParameter("divisor", type)
        }
        klass.irStaticFunction {
            name = Name.identifier("remainderUnsigned")
            returnType = type
            valueParameter("dividend", type)
            valueParameter("divisor", type)
        }
        klass.irStaticFunction {
            name = Name.identifier("toUnsignedString")
            returnType = irBuiltIns.stringType
            valueParameter("i", type)
        }
    }

    val signatureStringIntrinsic: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<signature-string>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            valueParameter("v", irBuiltIns.anyNType)
            returnType = irBuiltIns.stringType
        }.symbol

    private val javaLangString: IrClassSymbol =
        createClass(FqName("java.lang.String")) { klass ->
            val valueOfTypes = with(irBuiltIns) {
                listOf(anyNType, booleanType, charType, intType, longType, floatType, doubleType)
            }

            for (type in valueOfTypes) {
                klass.irStaticFunction {
                    name = Name.identifier("valueOf")
                    returnType = irBuiltIns.stringType
                    valueParameter("value", type)
                }
            }
        }

    private val defaultValueOfFunction = javaLangString.functions.single {
        it.owner.name.asString() == "valueOf"
                && it.owner.hasShape(regularParameters = 1, parameterTypes = listOf(irBuiltIns.anyNType))
    }

    private val valueOfFunctions: Map<IrType, IrSimpleFunctionSymbol?> =
        context.irBuiltIns.primitiveIrTypes.associateWith { type ->
            javaLangString.functions.singleOrNull {
                it.owner.name.asString() == "valueOf"
                        && it.owner.hasShape(regularParameters = 1, parameterTypes = listOf(type))
            }
        }

    fun typeToStringValueOfFunction(type: IrType): IrSimpleFunctionSymbol =
        valueOfFunctions[type] ?: defaultValueOfFunction

    private val javaLangEnum: IrClassSymbol =
        createClass(FqName("java.lang.Enum")) { klass ->
            // The declaration of Enum.valueOf is: `public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name)`
            // But we only need the following type-erased version to generate correct calls.
            klass.irStaticFunction {
                name = Name.identifier("valueOf")
                returnType = klass.defaultType
                valueParameter("enumType", javaLangClass.starProjectedType)
                valueParameter("name", irBuiltIns.stringType)
            }
        }

    val enumValueOfFunction: IrSimpleFunctionSymbol =
        javaLangEnum.functionByName("valueOf")

    private val javaLangObject: IrClassSymbol =
        createClass(FqName("java.lang.Object")) { klass ->
            klass.irFunction {
                name = Name.identifier("clone")
                returnType = irBuiltIns.anyType
            }
        }

    val objectCloneFunction: IrSimpleFunctionSymbol =
        javaLangObject.functionByName("clone")

    private val kotlinCoroutinesJvmInternalRunSuspendKt =
        createClass(FqName("kotlin.coroutines.jvm.internal.RunSuspendKt")) { klass ->
            klass.irStaticFunction {
                name = Name.identifier("runSuspend")
                returnType = irBuiltIns.unitType
                valueParameter(
                    "block",
                    getJvmSuspendFunctionClass(0).typeWith(
                        irBuiltIns.unitType
                    )
                )
            }
        }

    val runSuspendFunction: IrSimpleFunctionSymbol =
        kotlinCoroutinesJvmInternalRunSuspendKt.functionByName("runSuspend")

    val repeatableContainer: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.RepeatableContainer"), ClassKind.ANNOTATION_CLASS).apply {
            owner.irConstructor { isPrimary = true }
        }

    val javaAnnotations = JavaAnnotations()

    inner class JavaAnnotations {
        private val javaLangAnnotation: FqName = FqName("java.lang.annotation")

        private val javaLangAnnotationPackage: IrPackageFragment =
            createEmptyExternalPackageFragment(irModule, javaLangAnnotation)

        private fun buildClass(
            fqName: FqName,
            classKind: ClassKind = ClassKind.ANNOTATION_CLASS,
        ): IrClass = context.irFactory.irClass {
            check(fqName.parent() == javaLangAnnotation) { fqName }
            name = fqName.shortName()
            kind = classKind
            parent = javaLangAnnotationPackage
            thisReceiver()
        }.apply {
            javaLangAnnotationPackage.addChild(this)
        }

        private fun buildAnnotationConstructor(annotationClass: IrClass, valueType: IrType? = null): IrConstructor =
            annotationClass.irConstructor {
                isPrimary = true
                if (valueType != null) {
                    valueParameter {
                        name = Name.identifier("value")
                        type = valueType
                        origin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                    }
                }
            }

        private fun buildEnumEntry(enumClass: IrClass, entryName: String): IrEnumEntry {
            return context.irFactory.createEnumEntry(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB,
                Name.identifier(entryName),
                IrEnumEntrySymbolImpl(),
            ).apply {
                parent = enumClass
                enumClass.addChild(this)
            }
        }

        val documentedConstructor = buildAnnotationConstructor(buildClass(JvmAnnotationNames.DOCUMENTED_ANNOTATION))

        val retentionPolicyEnum = buildClass(JvmAnnotationNames.RETENTION_POLICY_ENUM, classKind = ClassKind.ENUM_CLASS)
        val rpRuntime = buildEnumEntry(retentionPolicyEnum, "RUNTIME")

        val retentionConstructor = buildAnnotationConstructor(buildClass(JvmAnnotationNames.RETENTION_ANNOTATION), retentionPolicyEnum.defaultType)

        val elementTypeEnum = buildClass(JvmAnnotationNames.ELEMENT_TYPE_ENUM, classKind = ClassKind.ENUM_CLASS)
        private val etMethod = buildEnumEntry(elementTypeEnum, "METHOD")

        val targetConstructor = buildAnnotationConstructor(buildClass(JvmAnnotationNames.TARGET_ANNOTATION), elementTypeEnum.defaultType)

        val repeatableConstructor = buildAnnotationConstructor(buildClass(JvmAnnotationNames.REPEATABLE_ANNOTATION), irBuiltIns.kClassClass.starProjectedType)

        val annotationRetentionMap = mapOf(
            KotlinRetention.SOURCE to buildEnumEntry(retentionPolicyEnum, "SOURCE"),
            KotlinRetention.BINARY to buildEnumEntry(retentionPolicyEnum, "CLASS"),
            KotlinRetention.RUNTIME to rpRuntime
        )

        val jvmTargetMap = mutableMapOf(
            KotlinTarget.CLASS to buildEnumEntry(elementTypeEnum, "TYPE"),
            KotlinTarget.ANNOTATION_CLASS to buildEnumEntry(elementTypeEnum, "ANNOTATION_TYPE"),
            KotlinTarget.CONSTRUCTOR to buildEnumEntry(elementTypeEnum, "CONSTRUCTOR"),
            KotlinTarget.LOCAL_VARIABLE to buildEnumEntry(elementTypeEnum, "LOCAL_VARIABLE"),
            KotlinTarget.FUNCTION to etMethod,
            KotlinTarget.PROPERTY_GETTER to etMethod,
            KotlinTarget.PROPERTY_SETTER to etMethod,
            KotlinTarget.FIELD to buildEnumEntry(elementTypeEnum, "FIELD"),
            KotlinTarget.BACKING_FIELD to buildEnumEntry(elementTypeEnum, "FIELD"),
            KotlinTarget.VALUE_PARAMETER to buildEnumEntry(elementTypeEnum, "PARAMETER")
        )

        val typeParameterTarget = buildEnumEntry(elementTypeEnum, "TYPE_PARAMETER")
        val typeUseTarget = buildEnumEntry(elementTypeEnum, "TYPE_USE")
    }

    companion object {
        const val INTRINSICS_CLASS_NAME = "kotlin/jvm/internal/Intrinsics"

        val CONTEXT_ARGUMENTS_PARAMETER_NAME: Name = Name.identifier("contextArguments")

        val RECEIVER_PARAMETER_NAME: Name = Name.identifier("receiver")

        val BOUND_CONTEXT_ARGUMENTS_FIELD_NAME: Name = Name.identifier("boundContextArguments")

        val FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME: FqName =
            StandardClassIds.Annotations.FlexibleNullability.asSingleFqName()

        val FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME: FqName =
            StandardClassIds.Annotations.FlexibleMutability.asSingleFqName()

        val RAW_TYPE_ANNOTATION_FQ_NAME: FqName =
            StandardClassIds.Annotations.RawTypeAnnotation.asSingleFqName()

        val FLEXIBLE_VARIANCE_ANNOTATION_FQ_NAME: FqName =
            StandardClassIds.Annotations.FlexibleArrayElementVariance.asSingleFqName()
    }
}

/**
 * Selects the constructor of a callable reference superclass ([JvmSymbols.functionReferenceImpl],
 * [JvmSymbols.adaptedFunctionReference], or a `(Mutable)PropertyReferenceNImpl` class) matching the given reference shape:
 * `[arity,] [contextArguments,] [receiver,] owner, name, signature, flags`.
 */
fun IrClass.callableReferenceSuperConstructor(hasBoundContextArguments: Boolean, hasBoundReceiver: Boolean): IrConstructor =
    constructors.single { constructor ->
        constructor.parameters.any { it.name == JvmSymbols.CONTEXT_ARGUMENTS_PARAMETER_NAME } == hasBoundContextArguments &&
                constructor.parameters.any { it.name == JvmSymbols.RECEIVER_PARAMETER_NAME } == hasBoundReceiver
    }

fun IrClassSymbol.functionByName(name: String): IrSimpleFunctionSymbol =
    functions.single { it.owner.name.asString() == name }

fun IrClassSymbol.fieldByName(name: String): IrFieldSymbol =
    fields.single { it.owner.name.asString() == name }
