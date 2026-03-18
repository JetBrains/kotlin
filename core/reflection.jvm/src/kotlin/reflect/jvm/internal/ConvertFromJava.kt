/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.load.java.FakePureImplementationsProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.internal.types.FlexibleKType
import kotlin.reflect.jvm.internal.types.SimpleKType
import kotlin.reflect.jvm.internal.types.getMutableCollectionKClass
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure

internal fun Type.toKType(
    knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>,
    nullability: TypeNullability = TypeNullability.FLEXIBLE,
    isForAnnotationParameter: Boolean = false,
    replaceNonArrayArgumentsWithStarProjections: Boolean = false,
    howThisTypeIsUsed: TypeUsage = TypeUsage.COMMON,
): KType {
    if (this == Void.TYPE) return StandardKTypes.UNIT_RETURN_TYPE

    val base: SimpleKType = when (this) {
        is Class<*> -> {
            if (allTypeParameters().isNotEmpty() && !replaceNonArrayArgumentsWithStarProjections) {
                return createRawJavaType(this, knownTypeParameters, isForAnnotationParameter)
            }
            if (isArray) {
                val argumentType = componentType.toKTypeProjection(knownTypeParameters, isForAnnotationParameter)
                return createJavaSimpleType(this, kotlin, listOf(argumentType), isMarkedNullable = false)
                    .toFlexibleArrayType(this, nullability, isForAnnotationParameter)
            }
            createJavaSimpleType(this, kotlin, allTypeParameters().map { KTypeProjection.STAR }, isMarkedNullable = false)
        }
        is GenericArrayType -> {
            val componentType = genericComponentType.toKTypeProjection(knownTypeParameters, isForAnnotationParameter)
            val componentClass = componentType.type!!.jvmErasure.java.createArrayType().kotlin
            return createJavaSimpleType(this, componentClass, listOf(componentType), isMarkedNullable = false)
                .toFlexibleArrayType(this, nullability, isForAnnotationParameter)
        }
        is ParameterizedType -> createJavaSimpleType(
            this,
            (rawType as Class<*>).convertJavaClass(isForAnnotationParameter),
            if (replaceNonArrayArgumentsWithStarProjections)
                collectAllArguments().map { KTypeProjection.STAR }
            else
                collectAllArguments().map { it.toKTypeProjection(knownTypeParameters, isForAnnotationParameter) },
            isMarkedNullable = false,
        )
        is TypeVariable<*> ->
            createJavaSimpleType(this, findKTypeParameterInContainer(knownTypeParameters), emptyList(), isMarkedNullable = false)
        is WildcardType -> throw KotlinReflectionInternalError("Wildcard type is not possible here: $this")
        else -> throw KotlinReflectionInternalError("Type is not supported: $this (${this::class.java})")
    }

    if (isForAnnotationParameter) return base

    // We cannot read `@kotlin.annotations.jvm.Mutable/ReadOnly` annotations in kotlin-reflect because they have CLASS retention.
    // Therefore, collection types in Java are considered mutability-flexible by default. The few exceptions are listed a bit below.
    val mutableType = base.createMutableCollectionType(this)

    // Java collection type is loaded as mutable (as opposed to mutability-flexible) in the following cases:
    // 1) If it's the top-level type in the supertype position.
    // 2) If its last type argument has a contravariant projection. `List<in A>` does not make sense, but `MutableList<in A>` does.
    //    So, `java.util.List<? super X>` is transformed to `kotlin.collections.MutableList<in X>` (NOT `(Mutable)List<in X>`).
    //    Similarly, `java.util.Map<K, ? super V>` is transformed to `kotlin.collections.MutableMap<K, in V>`.
    val withMutableFlexibility =
        if (howThisTypeIsUsed == TypeUsage.SUPERTYPE || argumentsMakeSenseOnlyForMutableContainer(mutableType)) mutableType ?: base
        else mutableType?.let {
            FlexibleKType.create(it, base, isRawType = false) { this }
        } ?: base

    return when (nullability) {
        TypeNullability.NOT_NULL -> withMutableFlexibility
        TypeNullability.NULLABLE -> withMutableFlexibility.makeNullableAsSpecified(nullable = true)
        TypeNullability.FLEXIBLE ->
            if (this is Class<*> && isPrimitive) base
            else FlexibleKType.create(
                lowerBound = withMutableFlexibility.lowerBoundIfFlexible() ?: withMutableFlexibility,
                upperBound = (withMutableFlexibility.upperBoundIfFlexible()
                    ?: withMutableFlexibility).makeNullableAsSpecified(nullable = true),
                isRawType = false,
            ) { this }
    }
}

internal enum class TypeNullability {
    NOT_NULL,
    NULLABLE,
    FLEXIBLE,
}

internal enum class TypeUsage {
    SUPERTYPE,
    COMMON,
}

private fun createJavaSimpleType(
    type: Type,
    classifier: KClassifier,
    arguments: List<KTypeProjection>,
    isMarkedNullable: Boolean,
    mutableCollectionClass: KClass<*>? = null,
): SimpleKType = SimpleKType(
    classifier, arguments, isMarkedNullable,
    annotations = emptyList(),
    abbreviation = null,
    isDefinitelyNotNullType = false,
    isNothingType = false,
    isSuspendFunctionType = false,
    mutableCollectionClass = mutableCollectionClass,
    computeJavaType = { type },
)

private fun createRawJavaType(
    jClass: Class<*>, knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>, isForAnnotationParameter: Boolean,
): KType {
    val kClass = jClass.convertJavaClass(isForAnnotationParameter)
    val lowerBound = createJavaSimpleType(
        jClass, kClass,
        jClass.allTypeParameters().map { typeParameter ->
            // When creating a lower bound for a raw type, we must take the corresponding bound of each type parameter, but erase their
            // type arguments to star projections. E.g. `T : Comparable<String>` becomes `Comparable<*>`. We have to be very careful not
            // to translate the bound's own type parameters because it will lead to stack overflow in cases like `class A<T extends A>`.
            // Since a type parameter's upper bound may be another type parameter, we need to unwrap it until we end up with anything
            // but the type parameter (`Class` or `ParameterizedType`).
            // Note that this is still not exactly how the compiler translates raw types
            // (see `JavaClassifierType.toConeKotlinTypeForFlexibleBound` in K2, or `JavaTypeResolver.computeRawTypeArguments` in K1),
            // but it's a good enough approximation.
            val upperBound = generateSequence(typeParameter) { it.bounds.first() as? TypeVariable<*> }.last().bounds.first()
            KTypeProjection.invariant(upperBound.toKType(knownTypeParameters, replaceNonArrayArgumentsWithStarProjections = true))
        },
        isMarkedNullable = false,
    ).let {
        it.createMutableCollectionType(jClass) ?: it
    }
    val upperBound = createJavaSimpleType(
        jClass, kClass, jClass.allTypeParameters().map { KTypeProjection.STAR }, isMarkedNullable = true,
    )
    return FlexibleKType.create(lowerBound, upperBound, isRawType = true) { jClass }
}

private fun SimpleKType.createMutableCollectionType(javaType: Type): SimpleKType? {
    val klass = classifier as? KClass<*> ?: return null
    val mutableFqName = JavaToKotlinClassMap.readOnlyToMutable(klass.qualifiedName?.let(::FqNameUnsafe)) ?: return null
    return createJavaSimpleType(
        javaType, classifier, arguments, isMarkedNullable,
        mutableCollectionClass = getMutableCollectionKClass(mutableFqName, klass),
    )
}

private fun Class<*>.convertJavaClass(isForAnnotationParameter: Boolean): KClass<*> =
    if (isForAnnotationParameter && this == Class::class.java) KClass::class
    else this.kotlin

internal fun Class<*>.allTypeParameters(): List<TypeVariable<*>> =
    generateSequence(this) {
        if (!Modifier.isStatic(it.modifiers)) it.declaringClass else null
    }.flatMap { it.typeParameters.asSequence() }.toList()

private fun ParameterizedType.collectAllArguments(): List<Type> =
    generateSequence(this) { it.ownerType as? ParameterizedType }.flatMap { it.actualTypeArguments.toList() }.toList()

private fun Type.toKTypeProjection(
    knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>, isForAnnotationParameter: Boolean,
): KTypeProjection {
    if (this !is WildcardType) {
        return KTypeProjection.invariant(toKType(knownTypeParameters, isForAnnotationParameter = isForAnnotationParameter))
    }

    val upperBounds = upperBounds
    val lowerBounds = lowerBounds
    if (upperBounds.size > 1 || lowerBounds.size > 1) {
        throw KotlinReflectionInternalError("Wildcard types with many bounds are not supported: $this")
    }
    return when {
        lowerBounds.size == 1 -> KTypeProjection.contravariant(
            lowerBounds.single().toKType(knownTypeParameters, isForAnnotationParameter = isForAnnotationParameter)
        )
        upperBounds.size == 1 -> upperBounds.single().let {
            if (it == Any::class.java) KTypeProjection.STAR
            else KTypeProjection.covariant(
                upperBounds.single().toKType(knownTypeParameters, isForAnnotationParameter = isForAnnotationParameter)
            )
        }
        else -> KTypeProjection.STAR
    }
}

private val TypeVariable<*>.kotlinContainer: KTypeParameterOwnerImpl
    get() = when (val container = genericDeclaration) {
        is Class<*> -> container.kotlin as KClassImpl<*>
        is Constructor<*> -> {
            val constructedClass = container.declaringClass.kotlin as KClassImpl<*>
            constructedClass.constructors.singleOrNull { it.javaConstructor == container } as JavaKConstructor?
                ?: throw KotlinReflectionInternalError(
                    "Constructor $container is not found in $constructedClass:\n" +
                            constructedClass.constructors.joinToString("\n") { "  - $it (${it.javaConstructor})" }
                )
        }
        is Method -> {
            require(Modifier.isStatic(container.modifiers)) {
                "Only static methods are supported for now: $container"
            }
            val containingClass = container.declaringClass.kotlin as KClassImpl<*>
            containingClass.functions.singleOrNull { it.javaMethod == container } as JavaKFunction?
                ?: throw KotlinReflectionInternalError(
                    "Method $container is not found in $containingClass:\n" +
                            containingClass.functions.joinToString("\n") { "  - $it (${it.javaMethod})" }
                )
        }
        else -> throw KotlinReflectionInternalError("Unsupported container of a type parameter: $container ($this)")
    }

// The map `knownTypeParameters` is needed because when we're computing upper bounds of type parameters for a Java class, there's a moment
// when the KTypeParameter instances are already created, but not yet stored in `KClass.typeParameters`.
private fun TypeVariable<*>.findKTypeParameterInContainer(knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>): KTypeParameter =
    knownTypeParameters[this]
        ?: kotlinContainer.typeParameters.singleOrNull { it.name == name }
        ?: throw KotlinReflectionInternalError("Type parameter $name is not found in $kotlinContainer")

internal fun Array<out TypeVariable<*>>.toKTypeParameters(container: KTypeParameterOwnerImpl): List<KTypeParameter> {
    val kTypeParameters = this.associateWith {
        KTypeParameterImpl(container, it.name, KVariance.INVARIANT, isReified = false)
    }
    for ((typeVariable, kTypeParameter) in kTypeParameters) {
        kTypeParameter.upperBounds = typeVariable.bounds.map { it.toKType(kTypeParameters) }
    }
    return kTypeParameters.values.toList()
}

private fun SimpleKType.toFlexibleArrayType(
    javaType: Type,
    nullability: TypeNullability,
    isForAnnotationParameter: Boolean,
): KType =
    if (isForAnnotationParameter) this
    else FlexibleKType.create(
        lowerBound = this,
        upperBound = createJavaSimpleType(
            javaType, classifier, arguments.map { it.type?.let(KTypeProjection::covariant) ?: it },
            isMarkedNullable = nullability != TypeNullability.NOT_NULL,
        ),
        isRawType = false,
        computeJavaType = { javaType },
    ) as FlexibleKType

private fun Type.argumentsMakeSenseOnlyForMutableContainer(mutableType: SimpleKType?): Boolean =
    this is ParameterizedType && actualTypeArguments.last().let {
        it is WildcardType && it.lowerBounds.size == 1
    } && mutableType != null && (mutableType.classifier as KClass<*>).typeParameters.last().variance == KVariance.OUT

internal fun Int.computeVisibilityForJavaModifiers(): KVisibility? = when {
    Modifier.isPublic(this) -> KVisibility.PUBLIC
    Modifier.isPrivate(this) -> KVisibility.PRIVATE
    // Java's protected also allows access in the same package, so it's not the same as Kotlin's protected.
    else -> null
}

internal fun Member.isEnumValuesValueOfMethod(): Boolean =
    this is Method && declaringClass.isEnum && Modifier.isStatic(modifiers) && (
            (name == "values" && parameterTypes.size == 0) ||
                    (name == "valueOf" && parameterTypes.singleOrNull() == String::class.java)
            )

internal fun getPurelyImplementedSupertype(kClass: KClassImpl<*>): KType? {
    val annotatedPurelyImplementedClassId =
        kClass.findAnnotation<PurelyImplements>()?.value?.let(::FqName)?.takeIf { fqName ->
            !fqName.isRoot && fqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
        }?.let(ClassId::topLevel)

    val purelyImplementedClassId =
        annotatedPurelyImplementedClassId
            ?: FakePureImplementationsProvider.getPurelyImplementedInterface(kClass.classId)
            ?: return null

    val superClass = kClass.java.safeClassLoader.loadClass(purelyImplementedClassId) ?: return null

    val supertypeParameterCount = superClass.allTypeParameters().size
    val typeParameters = kClass.typeParameters
    val typeParameterCount = typeParameters.size

    @Suppress("IntroduceWhenSubject")
    val typeArguments = when {
        typeParameterCount == supertypeParameterCount ->
            typeParameters.map { parameter ->
                KTypeProjection.invariant(parameter.createType())
            }
        typeParameterCount == 1 && supertypeParameterCount > 1 && annotatedPurelyImplementedClassId == null -> {
            val parameter = KTypeProjection.invariant(typeParameters.single().createType())
            List(supertypeParameterCount) { parameter }
        }
        else -> return null
    }

    val result = createJavaSimpleType(superClass, superClass.kotlin, typeArguments, isMarkedNullable = false)
    return result.createMutableCollectionType(superClass) ?: result
}
