/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform

sealed class StubType {
    abstract val nullable: Boolean
}

/**
 * Wrapper over [Classifier].
 * @property underlyingType is not null if this type is an typealias.
 */
class ClassifierStubType(
        val classifier: Classifier,
        val typeArguments: List<TypeArgument> = emptyList(),
        val underlyingType: StubType? = null,
        override val nullable: Boolean = false
) : StubType() {
    fun nested(name: String): ClassifierStubType {
        assert(underlyingType == null) {
            "Cannot access nested class `$name` of typealias ${classifier.fqName}"
        }
        return ClassifierStubType(classifier.nested(name))
    }
}

/**
 * @return type from kotlinx.cinterop package
 */
fun KotlinPlatform.getRuntimeType(name: String, nullable: Boolean = false): StubType {
    val classifier = Classifier.topLevel("kotlinx.cinterop", name)
    PredefinedTypesHandler.tryExpandPlatformDependentTypealias(classifier, this, nullable)?.let { return it }
    return ClassifierStubType(classifier, nullable = nullable)
}

/**
 * Functional type from kotlin package: ([parameterTypes]) -> [returnType]
 */
class FunctionalType(
      val parameterTypes: List<StubType>,
      val returnType: StubType,
      override val nullable: Boolean = false
) : StubType() {
    val classifier: Classifier =
            Classifier.topLevel("kotlin", "Function${parameterTypes.size}")
}

class TypeParameterType(
        val name: String,
        override val nullable: Boolean
) : StubType()

fun KotlinType.toStubIrType(): StubType = when (this) {
    is KotlinFunctionType -> this.toStubIrType()
    is KotlinClassifierType -> this.toStubIrType()
    else -> error("Unexpected KotlinType: $this")
}

private fun KotlinFunctionType.toStubIrType(): StubType =
        FunctionalType(parameterTypes.map { it.toStubIrType() }, returnType.toStubIrType(), nullable)

private fun KotlinClassifierType.toStubIrType(): StubType {
    PredefinedTypesHandler.tryExpandPredefinedTypealias(classifier, nullable)?.let { return it }
    val typeArguments = arguments.map { it.toStubIrType() }
    val underlyingType = underlyingType?.toStubIrType()
    return ClassifierStubType(classifier, typeArguments, underlyingType, nullable)
}

private fun KotlinTypeArgument.toStubIrType(): TypeArgument = when (this) {
    is KotlinType -> TypeArgumentStub(this.toStubIrType())
    StarProjection -> TypeArgument.StarProjection
    else -> error("Unexpected KotlinTypeArgument: $this")
}

/**
 * Types that come from kotlinx.cinterop require special handling because we
 * don't have explicit information about their structure.
 * For example, to be able to produce metadata-based interop library we need to know
 * that ByteVar is a typealias to ByteVarOf<Byte>.
 */
private object PredefinedTypesHandler {
    private const val cInteropPackage = "kotlinx.cinterop"

    private val nativePtrClassifier = Classifier.topLevel(cInteropPackage, "NativePtr")

    private val primitives = setOf(
            KotlinTypes.boolean,
            KotlinTypes.byte, KotlinTypes.short, KotlinTypes.int, KotlinTypes.long,
            KotlinTypes.uByte, KotlinTypes.uShort, KotlinTypes.uInt, KotlinTypes.uLong,
            KotlinTypes.float, KotlinTypes.double
    )

    /**
     * kotlinx.cinterop.{primitive}Var -> kotlin.{primitive}
     */
    private val primitiveVarClassifierToPrimitiveType: Map<Classifier, KotlinClassifierType> =
            primitives.associateBy {
                val typeVar = "${it.classifier.topLevelName}Var"
                Classifier.topLevel(cInteropPackage, typeVar)
            }

    /**
     * @param primitiveType primitive type from kotlin package.
     * @return kotlinx.cinterop.[primitiveType]VarOf<[primitiveType]>
     */
    private fun getVarOfTypeFor(primitiveType: KotlinClassifierType, nullable: Boolean): ClassifierStubType {
        val typeVarOf = "${primitiveType.classifier.topLevelName}VarOf"
        val classifier = Classifier.topLevel(cInteropPackage, typeVarOf)
        return ClassifierStubType(classifier, listOf(TypeArgumentStub(primitiveType.toStubIrType())), nullable = nullable)
    }

    private fun expandCOpaquePointerVar(nullable: Boolean): ClassifierStubType {
        val typeArgument = TypeArgumentStub(expandCOpaquePointer(nullable=false))
        val underlyingType = ClassifierStubType(
                KotlinTypes.cPointerVarOf, listOf(typeArgument), nullable = nullable
        )
        return ClassifierStubType(
                KotlinTypes.cOpaquePointerVar.classifier, underlyingType = underlyingType, nullable = nullable
        )
    }

    private fun expandCOpaquePointer(nullable: Boolean): ClassifierStubType {
        val typeArgument = TypeArgumentStub(ClassifierStubType(KotlinTypes.cPointed), TypeArgument.Variance.OUT)
        val underlyingType = ClassifierStubType(
                KotlinTypes.cPointer, listOf(typeArgument), nullable = nullable
        )
        return ClassifierStubType(
                KotlinTypes.cOpaquePointer.classifier, underlyingType = underlyingType, nullable = nullable
        )
    }

    /**
     * @param primitiveVarType one of kotlinx.cinterop.{primitive}Var types.
     * @return typealias in terms of StubIR types.
     */
    private fun expandPrimitiveVarType(primitiveVarClassifier: Classifier, nullable: Boolean): ClassifierStubType {
        val primitiveType = primitiveVarClassifierToPrimitiveType.getValue(primitiveVarClassifier)
        val underlyingType = getVarOfTypeFor(primitiveType, nullable)
        return ClassifierStubType(primitiveVarClassifier, underlyingType = underlyingType, nullable = nullable)
    }

    private fun expandNativePtr(platform: KotlinPlatform, nullable: Boolean): ClassifierStubType {
        val underlyingTypeClassifier = when (platform) {
            KotlinPlatform.JVM -> KotlinTypes.long.classifier
            KotlinPlatform.NATIVE -> Classifier.topLevel("kotlin.native.internal", "NativePtr")
        }
        val underlyingType = ClassifierStubType(underlyingTypeClassifier, nullable = nullable)
        return ClassifierStubType(nativePtrClassifier, underlyingType = underlyingType, nullable = nullable)
    }

    /**
     * @return [ClassifierStubType] if [classifier] is a typealias from [kotlinx.cinterop] package.
     */
    fun tryExpandPredefinedTypealias(classifier: Classifier, nullable: Boolean): ClassifierStubType? =
            when (classifier) {
                in primitiveVarClassifierToPrimitiveType.keys -> expandPrimitiveVarType(classifier, nullable)
                KotlinTypes.cOpaquePointer.classifier -> expandCOpaquePointer(nullable)
                KotlinTypes.cOpaquePointerVar.classifier -> expandCOpaquePointerVar(nullable)
                else -> null
            }

    /**
     * Variant of [tryExpandPredefinedTypealias] with [platform]-dependent result.
     */
    fun tryExpandPlatformDependentTypealias(
            classifier: Classifier, platform: KotlinPlatform, nullable: Boolean
    ): ClassifierStubType? =
            when (classifier) {
                nativePtrClassifier -> expandNativePtr(platform, nullable)
                else -> null
            }
}