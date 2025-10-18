/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.runtime.structure.parameterizedTypeArguments
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.coroutines.Continuation
import kotlin.metadata.*
import kotlin.metadata.jvm.annotations
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.createAnnotationInstance
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.SimpleKType
import kotlin.reflect.jvm.internal.types.getMutableCollectionKClass
import kotlin.reflect.jvm.jvmErasure

internal fun ClassName.toClassId(): ClassId {
    val isLocal = startsWith(".")
    val fullName = if (isLocal) substring(1) else this
    return ClassId(
        FqName(fullName.substringBeforeLast('/', "").replace('/', '.')),
        FqName(fullName.substringAfterLast('/')),
        isLocal,
    )
}

internal fun ClassName.toNonLocalSimpleName(): String {
    require(!startsWith(".")) { "Local class is not supported: $this" }
    return substringAfterLast('/').substringAfterLast('.')
}

internal fun ClassLoader.loadKClass(name: ClassName): KClass<*>? =
    loadClass(name.toClassId())?.kotlin

/**
 * Provides the access to the type parameters of a Kotlin declaration, and allows to obtain a type parameter given its id.
 *
 * @property ownTypeParameters the list of type parameters of this declaration. In case of a class member or an inner class, does not
 *   include type parameters of the enclosing class.
 * @property map the mapping from type parameter "id" to [KTypeParameter] objects. Note that the integer key is not the type parameter's
 *   index, it's the **id** as returned by [KmTypeParameter.id].
 * @property parent the type parameter table of the enclosing declaration, or `null` if there's none.
 */
internal class TypeParameterTable private constructor(
    val ownTypeParameters: List<KTypeParameterImpl>,
    private val map: Map<Int, KTypeParameter>,
    private val parent: TypeParameterTable?,
) {
    /**
     * Provides the mapping from type parameter "id" ([KmTypeParameter.id]) to [KTypeParameter] objects, allowing to look for
     * type parameters not only in the immediate container, but also its containers.
     */
    operator fun get(id: Int): KTypeParameter? = map[id] ?: parent?.get(id)

    companion object {
        @JvmField
        val EMPTY = TypeParameterTable(emptyList(), emptyMap(), null)

        fun create(
            kmTypeParameters: List<KmTypeParameter>,
            parent: TypeParameterTable?,
            container: KTypeParameterOwnerImpl,
            classLoader: ClassLoader,
        ): TypeParameterTable {
            val kTypeParameters = kmTypeParameters.map { km ->
                KTypeParameterImpl(container, km.name, km.variance.toKVariance(), km.isReified)
            }
            val map = kmTypeParameters.withIndex().associate { (index, km) -> km.id to kTypeParameters[index] }
            return TypeParameterTable(kTypeParameters, map, parent).also { table ->
                for ((i, typeParameter) in kTypeParameters.withIndex()) {
                    typeParameter.upperBounds = kmTypeParameters[i].upperBounds.map { it.toKType(classLoader, table) }
                        .ifEmpty { listOf(StandardKTypes.NULLABLE_ANY) }
                }
            }
        }
    }
}

internal fun KmType.toKType(
    classLoader: ClassLoader,
    typeParameterTable: TypeParameterTable,
    computeJavaType: (() -> Type)? = null,
): KType {
    lateinit var result: SimpleKType
    val arguments = generateSequence(this) { it.outerType }
        .flatMap { it.arguments }
        .mapIndexed { i, typeArgument ->
            typeArgument.toKTypeProjection(
                classLoader, typeParameterTable,
                if (computeJavaType == null) null else convertTypeArgumentToJavaType({ result }, i)
            )
        }
        .toList()
    val kClassifier = classifier.toClassifier(classLoader, typeParameterTable, arguments)
    result = SimpleKType(
        kClassifier,
        arguments,
        isNullable,
        annotations.map { it.toAnnotation(classLoader) },
        abbreviatedType?.toKType(classLoader, typeParameterTable),
        isDefinitelyNonNull,
        (classifier as? KmClassifier.Class)?.name == "kotlin/Nothing",
        isSuspend,
        classifier.toMutableCollectionKClass(kClassifier),
        computeJavaType,
    )
    if (isSuspend) {
        // Suspend function types are represented in metadata in a non-trivial way, see kdoc on [KmType.isSuspend].
        result = unwrapSuspendFunctionType(result, computeJavaType)
            ?: throw KotlinReflectionInternalError("Invalid suspend function type: $result")
    }
    return result
}

private fun unwrapSuspendFunctionType(type: SimpleKType, computeJavaType: (() -> Type)?): SimpleKType? {
    require(type.isSuspendFunctionType) { "Not a suspend function type: $type" }
    val continuationArgument = type.arguments.getOrNull(type.arguments.size - 2)?.type ?: return null
    if (continuationArgument.classifier != Continuation::class) return null
    val returnType = continuationArgument.arguments.singleOrNull()?.type ?: return null
    return SimpleKType(
        type.classifier,
        type.arguments.dropLast(2) + KTypeProjection.invariant(returnType),
        type.isMarkedNullable,
        type.annotations,
        type.abbreviation,
        type.isDefinitelyNotNullType,
        type.isNothingType,
        isSuspendFunctionType = true,
        type.mutableCollectionClass,
        computeJavaType,
    )
}

internal fun convertTypeArgumentToJavaType(computeType: () -> AbstractKType, index: Int): () -> Type = {
    val type = computeType()
    val javaParameterizedTypeArguments: List<Type> by lazy(PUBLICATION) { type.javaType!!.parameterizedTypeArguments }
    when (val javaType = type.javaType) {
        is Class<*> -> {
            // It's either an array or a raw type.
            // TODO: return upper bound of the corresponding parameter for a raw type?
            if (javaType.isArray) javaType.componentType else Any::class.java
        }
        is GenericArrayType -> {
            if (index != 0) throw KotlinReflectionInternalError("Array type has been queried for a non-0th argument: $type")
            javaType.genericComponentType
        }
        is ParameterizedType -> {
            val argument = javaParameterizedTypeArguments[index]
            // In "Foo<out Bar>", the JVM type of the first type argument should be "Bar", not "? extends Bar"
            if (argument !is WildcardType) argument
            else argument.lowerBounds.firstOrNull() ?: argument.upperBounds.first()
        }
        else -> throw KotlinReflectionInternalError("Non-generic type has been queried for arguments: $type")
    }
}

private fun KmClassifier.toClassifier(
    classLoader: ClassLoader, typeParameterTable: TypeParameterTable, typeArguments: List<KTypeProjection>,
): KClassifier = when (this) {
    is KmClassifier.Class ->
        if (name == "kotlin/Array")
            (typeArguments.single().type ?: StandardKTypes.ANY).jvmErasure.java.createArrayType().kotlin
        else
            classLoader.loadKClass(name) ?: throw KotlinReflectionInternalError("Class not found: $name")
    is KmClassifier.TypeAlias ->
        KTypeAliasImpl(name.toClassId().asSingleFqName())
    is KmClassifier.TypeParameter ->
        typeParameterTable[id] ?: throw KotlinReflectionInternalError("Type parameter not found: $id")
}

private fun KmTypeProjection.toKTypeProjection(
    classLoader: ClassLoader,
    typeParameterTable: TypeParameterTable,
    computeJavaType: (() -> Type)?,
): KTypeProjection =
    if (this == KmTypeProjection.STAR)
        KTypeProjection.STAR
    else
        KTypeProjection(variance?.toKVariance(), type?.toKType(classLoader, typeParameterTable, computeJavaType))

internal fun KmVariance.toKVariance(): KVariance = when (this) {
    KmVariance.IN -> KVariance.IN
    KmVariance.OUT -> KVariance.OUT
    KmVariance.INVARIANT -> KVariance.INVARIANT
}

private fun KmClassifier.toMutableCollectionKClass(kClassifier: KClassifier): MutableCollectionKClass<*>? {
    val classId = (this as? KmClassifier.Class)?.name?.toClassId() ?: return null
    if (!JavaToKotlinClassMap.isMutable(classId)) return null
    return getMutableCollectionKClass(classId.asSingleFqName(), kClassifier as KClass<*>)
}

internal fun KmAnnotation.toAnnotation(classLoader: ClassLoader): Annotation =
    createAnnotationInstance(
        classLoader.loadClass(className.toClassId())
            ?: throw KotlinReflectionInternalError("Annotation class not found: $className"),
        arguments.mapValues { (name, arg) -> arg.toAnnotationArgument(className, name, classLoader) },
    ) as Annotation

private fun KmAnnotationArgument.toAnnotationArgument(
    annotationClassName: ClassName, argumentName: String?, classLoader: ClassLoader,
): Any = when (this) {
    is KmAnnotationArgument.AnnotationValue -> annotation.toAnnotation(classLoader)
    is KmAnnotationArgument.ArrayKClassValue -> {
        var klass = classLoader.loadKClass(className)?.java
            ?: throw KotlinReflectionInternalError("Unresolved class: $className")
        repeat(arrayDimensionCount) {
            klass = klass.createArrayType()
        }
        klass
    }
    is KmAnnotationArgument.ArrayValue -> {
        // We need to create an array of a correct type, and for that we need to look up the type of the corresponding annotation parameter.
        val annotation = classLoader.loadKClass(annotationClassName)?.takeIf { it.java.isAnnotation }
            ?: throw KotlinReflectionInternalError("Not an annotation class: $annotationClassName")
        val parameterType = annotation.constructors.singleOrNull()?.parameters?.singleOrNull { it.name == argumentName }?.type
            ?: throw KotlinReflectionInternalError("No parameter $argumentName found in annotation constructor of $annotationClassName")
        val arrayClass = (parameterType.classifier as? KClass<*>)?.java
            ?: throw KotlinReflectionInternalError("Array parameter type is not a class: $parameterType")
        val componentType =
            if (arrayClass.componentType == KClass::class.java) Class::class.java else arrayClass.componentType
        java.lang.reflect.Array.newInstance(componentType, elements.size).also { array ->
            for ((index, element) in elements.withIndex()) {
                java.lang.reflect.Array.set(array, index, element.toAnnotationArgument(annotationClassName, null, classLoader))
            }
        }
    }
    is KmAnnotationArgument.EnumValue -> {
        val enumClass = classLoader.loadClass(enumClassName.toClassId())
            ?: throw KotlinReflectionInternalError("Unresolved enum class: $enumClassName")
        enumClass.enumConstants.singleOrNull { (it as Enum<*>).name == enumEntryName }
            ?: throw KotlinReflectionInternalError("Unresolved enum entry: $enumClassName.$enumEntryName")
    }
    is KmAnnotationArgument.KClassValue ->
        classLoader.loadClass(className.toClassId())
            ?: throw KotlinReflectionInternalError("Unresolved class: $className")
    is KmAnnotationArgument.LiteralValue<*> -> value
}

internal fun Visibility.toKVisibility(): KVisibility? = when (this) {
    Visibility.INTERNAL -> KVisibility.INTERNAL
    Visibility.PRIVATE -> KVisibility.PRIVATE
    Visibility.PROTECTED -> KVisibility.PROTECTED
    Visibility.PUBLIC -> KVisibility.PUBLIC
    Visibility.PRIVATE_TO_THIS -> KVisibility.PRIVATE
    Visibility.LOCAL -> null
}
