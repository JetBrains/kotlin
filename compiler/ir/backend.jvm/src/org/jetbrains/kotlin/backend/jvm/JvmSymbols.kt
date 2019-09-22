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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance

class JvmSymbols(
    context: JvmBackendContext,
    private val symbolTable: ReferenceSymbolTable,
    firMode: Boolean
) : Symbols<JvmBackendContext>(context, symbolTable) {
    private val storageManager = LockBasedStorageManager(this::class.java.simpleName)
    private val kotlinPackage: IrPackageFragment = createPackage(FqName("kotlin"))
    private val kotlinCoroutinesPackage: IrPackageFragment = createPackage(FqName("kotlin.coroutines"))
    private val kotlinJvmPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm"))
    private val kotlinJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.internal"))
    private val kotlinJvmFunctionsPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.functions"))
    private val javaLangPackage: IrPackageFragment = createPackage(FqName("java.lang"))

    private val irBuiltIns = context.irBuiltIns

    private val nullPointerExceptionClass: IrClass =
        createClass(FqName("java.lang.NullPointerException")) { klass ->
            klass.addConstructor().apply {
                addValueParameter("message", irBuiltIns.stringType)
            }
        }

    override val ThrowNullPointerException: IrFunction =
        nullPointerExceptionClass.constructors.single()

    override val ThrowNoWhenBranchMatchedException: IrFunction
        get() = error("Unused in JVM IR")

    private val typeCastExceptionClass: IrClass =
        createClass(FqName("kotlin.TypeCastException")) { klass ->
            klass.addConstructor().apply {
                addValueParameter("message", irBuiltIns.stringType)
            }
        }

    override val ThrowTypeCastException: IrFunction =
        typeCastExceptionClass.constructors.single()

    private fun createPackage(fqName: FqName): IrPackageFragment =
        IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(EmptyPackageFragmentDescriptor(context.state.module, fqName)))

    private fun createClass(fqName: FqName, classKind: ClassKind = ClassKind.CLASS, block: (IrClass) -> Unit = {}): IrClass =
        buildClass {
            name = fqName.shortName()
            kind = classKind
        }.apply {
            parent = when (fqName.parent().asString()) {
                "kotlin" -> kotlinPackage
                "kotlin.coroutines" -> kotlinCoroutinesPackage
                "kotlin.jvm.internal" -> kotlinJvmInternalPackage
                "kotlin.jvm.functions" -> kotlinJvmFunctionsPackage
                "java.lang" -> javaLangPackage
                else -> error("Other packages are not supported yet: $fqName")
            }
            createImplicitParameterDeclarationWithWrappedDescriptor()
            block(this)
        }

    private val intrinsicsClass: IrClass = createClass(
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
    }

    val checkExpressionValueIsNotNull: IrSimpleFunction =
        intrinsicsClass.functions.single { it.name.asString() == "checkExpressionValueIsNotNull" }

    val checkNotNullExpressionValue: IrSimpleFunction =
        intrinsicsClass.functions.single { it.name.asString() == "checkNotNullExpressionValue" }

    override val ThrowUninitializedPropertyAccessException: IrSimpleFunction =
        intrinsicsClass.functions.single { it.name.asString() == "throwUninitializedPropertyAccessException" }

    override val stringBuilder: IrClass
        get() = context.getTopLevelClass(FqName("java.lang.StringBuilder"))

    override val defaultConstructorMarker: IrClass =
        createClass(FqName("kotlin.jvm.internal.DefaultConstructorMarker"))

    override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunction>
        get() = error("Unused in JVM IR")

    override val coroutineImpl: IrClass
        get() = TODO("not implemented")

    override val coroutineSuspendedGetter: IrSimpleFunction
        get() = TODO("not implemented")

    override val getContinuation: IrSimpleFunction
        get() = TODO("not implemented")

    override val coroutineContextGetter: IrSimpleFunction
        get() = TODO("not implemented")

    override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunction
        get() = TODO("not implemented")

    override val coroutineGetContext: IrSimpleFunction
        get() = TODO("not implemented")

    override val returnIfSuspended: IrSimpleFunction
        get() = TODO("not implemented")

    val javaLangClass: IrClass =
        if (firMode) createClass(FqName("java.lang.Class")) else context.getTopLevelClass(FqName("java.lang.Class"))

    private val javaLangAssertionError: IrClass =
        if (firMode) createClass(FqName("java.lang.AssertionError")) else context.getTopLevelClass(FqName("java.lang.AssertionError"))

    val assertionErrorConstructor by lazy<IrConstructor> {
        javaLangAssertionError.constructors.single {
            it.valueParameters.size == 1 && it.valueParameters[0].type.isNullableAny()
        }
    }

    val continuationClass: IrClass =
        createClass(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE, ClassKind.INTERFACE) { klass ->
            klass.addTypeParameter("T", irBuiltIns.anyNType, Variance.IN_VARIANCE)
        }

    val lambdaClass: IrClass = createClass(FqName("kotlin.jvm.internal.Lambda")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
        }
    }

    private fun generateCallableReferenceMethods(klass: IrClass) {
        klass.addFunction("getSignature", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getName", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getOwner", irBuiltIns.kDeclarationContainerClass.typeWith(), Modality.OPEN)
    }

    val functionReference: IrClass = createClass(FqName("kotlin.jvm.internal.FunctionReference")) { klass ->
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

    val functionReferenceReceiverField: IrField = functionReference.fieldByName("receiver")
    val functionReferenceGetSignature: IrSimpleFunction = functionReference.functionByName("getSignature")
    val functionReferenceGetName: IrSimpleFunction = functionReference.functionByName("getName")
    val functionReferenceGetOwner: IrSimpleFunction = functionReference.functionByName("getOwner")

    fun getFunction(parameterCount: Int): IrClass =
        symbolTable.referenceClass(builtIns.getFunction(parameterCount))

    private val jvmFunctionClasses = storageManager.createMemoizedFunction { n: Int ->
        createClass(FqName("kotlin.jvm.functions.Function$n"), ClassKind.INTERFACE) { klass ->
            for (i in 1..n) {
                klass.addTypeParameter("P$i", irBuiltIns.anyNType, Variance.IN_VARIANCE)
            }
            val returnType = klass.addTypeParameter("R", irBuiltIns.anyNType, Variance.OUT_VARIANCE)

            klass.addFunction("invoke", returnType.defaultType, Modality.ABSTRACT).apply {
                for (i in 1..n) {
                    addValueParameter("p$i", klass.typeParameters[i - 1].defaultType)
                }
            }
        }
    }

    fun getJvmFunctionClass(parameterCount: Int): IrClass =
        jvmFunctionClasses(parameterCount)

    val functionN: IrClass = createClass(FqName("kotlin.jvm.functions.FunctionN"), ClassKind.INTERFACE) { klass ->
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

    fun getPropertyReferenceClass(mutable: Boolean, parameterCount: Int, impl: Boolean): IrClass =
        propertyReferenceClasses(PropertyReferenceKey(mutable, parameterCount, impl))

    val reflection: IrClass = createClass(FqName("kotlin.jvm.internal.Reflection")) { klass ->
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

    val getOrCreateKotlinPackage: IrSimpleFunction =
        reflection.functionByName("getOrCreateKotlinPackage")

    val desiredAssertionStatus: IrSimpleFunction by lazy {
        javaLangClass.functionByName("desiredAssertionStatus")
    }

    val unsafeCoerceIntrinsic: IrSimpleFunction =
        buildFun {
            name = Name.special("<unsafe-coerce>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }.apply {
            parent = kotlinJvmInternalPackage
            val src = addTypeParameter("T", irBuiltIns.anyNType)
            val dst = addTypeParameter("R", irBuiltIns.anyNType)
            addValueParameter("v", src.defaultType)
            returnType = dst.defaultType
        }

    private val collectionToArrayClass: IrClass = createClass(FqName("kotlin.jvm.internal.CollectionToArray")) { klass ->
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

    val nonGenericToArray: IrSimpleFunction =
        collectionToArrayClass.functions.single { it.name.asString() == "toArray" && it.valueParameters.size == 1 }

    val genericToArray: IrSimpleFunction =
        collectionToArrayClass.functions.single { it.name.asString() == "toArray" && it.valueParameters.size == 2 }

    val kClassJava: IrProperty =
        buildProperty {
            name = Name.identifier("java")
        }.apply {
            parent = kotlinJvmPackage
            addGetter().apply {
                addExtensionReceiver(irBuiltIns.kClassClass.typeWith())
                returnType = javaLangClass.typeWith()
            }
        }

    val spreadBuilder: IrClass = createClass(FqName("kotlin.jvm.internal.SpreadBuilder")) { klass ->
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

    val primitiveSpreadBuilders: Map<IrType, IrClass> = irBuiltIns.primitiveIrTypes.associateWith { irType ->
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

    private val systemClass: IrClass = createClass(FqName("java.lang.System")) { klass ->
        klass.addFunction("arraycopy", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("src", irBuiltIns.anyNType)
            addValueParameter("srcPos", irBuiltIns.intType)
            addValueParameter("dest", irBuiltIns.anyNType)
            addValueParameter("destPos", irBuiltIns.intType)
            addValueParameter("length", irBuiltIns.intType)
        }
    }

    val systemArraycopy: IrSimpleFunction = systemClass.functionByName("arraycopy")
}

private fun IrClass.functionByName(name: String): IrSimpleFunction =
    functions.single { it.name.asString() == name }
private fun IrClass.fieldByName(name: String): IrField =
    fields.single { it.name.asString() == name }
