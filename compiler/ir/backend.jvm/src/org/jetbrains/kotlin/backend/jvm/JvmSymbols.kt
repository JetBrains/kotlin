/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.Variance

class JvmSymbols(
    context: JvmBackendContext,
    private val symbolTable: ReferenceSymbolTable
) : Symbols<JvmBackendContext>(context, symbolTable) {
    private val storageManager = LockBasedStorageManager(this::class.java.simpleName)

    private val irBuiltIns = context.irBuiltIns

    override val ThrowNullPointerException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    override val ThrowNoWhenBranchMatchedException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    override val ThrowTypeCastException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    private fun createPackage(fqName: FqName): IrPackageFragment =
        IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(EmptyPackageFragmentDescriptor(context.state.module, fqName)))

    private val kotlinJvmInternalPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.internal"))
    private val kotlinJvmFunctionsPackage: IrPackageFragment = createPackage(FqName("kotlin.jvm.functions"))
    private val javaLangPackage: IrPackageFragment = createPackage(FqName("java.lang"))

    private fun createClass(fqName: FqName, classKind: ClassKind = ClassKind.CLASS, block: (IrClass) -> Unit): IrClass =
        buildClass {
            name = fqName.shortName()
            kind = classKind
        }.apply {
            parent = when (fqName.parent().asString()) {
                "kotlin.jvm.internal" -> kotlinJvmInternalPackage
                "kotlin.jvm.functions" -> kotlinJvmFunctionsPackage
                "java.lang" -> javaLangPackage
                else -> error("Other packages are not supported yet: $fqName")
            }
            createImplicitParameterDeclarationWithWrappedDescriptor()
            block(this)
        }

    private val intrinsicsClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Intrinsics")) { klass ->
        klass.addFunction("throwUninitializedPropertyAccessException", irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("propertyName", irBuiltIns.stringType)
        }
    }.symbol

    override val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
        intrinsicsClass.functions.single { it.owner.name.asString() == "throwUninitializedPropertyAccessException" }

    override val stringBuilder: IrClassSymbol
        get() = context.getTopLevelClass(FqName("java.lang.StringBuilder"))

    override val defaultConstructorMarker: IrClassSymbol =
        createClass(FqName("kotlin.jvm.internal.DefaultConstructorMarker")) { }.symbol

    override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
        get() = error("Unused in JVM IR")

    override val coroutineImpl: IrClassSymbol
        get() = TODO("not implemented")

    override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    override val getContinuation: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    val javaLangClass: IrClassSymbol = createClass(FqName("java.lang.Class")) {}.symbol

    val lambdaClass: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.Lambda")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
        }
    }.symbol

    private fun generateCallableReferenceMethods(klass: IrClass) {
        klass.addFunction("getSignature", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getName", irBuiltIns.stringType, Modality.OPEN)
        klass.addFunction("getOwner", irBuiltIns.kDeclarationContainerClass.typeWith(), Modality.OPEN)
    }

    val functionReference: IrClassSymbol = createClass(FqName("kotlin.jvm.internal.FunctionReference")) { klass ->
        klass.addConstructor().apply {
            addValueParameter("arity", irBuiltIns.intType)
            addValueParameter("receiver", irBuiltIns.anyNType)
        }

        generateCallableReferenceMethods(klass)
    }.symbol

    fun getFunction(parameterCount: Int): IrClassSymbol =
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
        }.symbol
    }

    fun getJvmFunctionClass(parameterCount: Int): IrClassSymbol =
        jvmFunctionClasses(parameterCount)

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
    }.symbol

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
        }.symbol
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
    }.symbol

    val getOrCreateKotlinPackage: IrSimpleFunctionSymbol =
        reflection.functions.single { it.owner.name.asString() == "getOrCreateKotlinPackage" }

    val getOrCreateKotlinClass: IrSimpleFunctionSymbol =
        reflection.functions.single { it.owner.name.asString() == "getOrCreateKotlinClass" }

    val getOrCreateKotlinClasses: IrSimpleFunctionSymbol =
        reflection.functions.single { it.owner.name.asString() == "getOrCreateKotlinClasses" }
}
