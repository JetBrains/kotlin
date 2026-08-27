/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.ClassKind
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext
import kotlin.reflect.jvm.internal.types.areEqualKTypes
import kotlin.reflect.jvm.javaField

private object CovariantOverrideComparator : Comparator<ReflectKCallable<*>> {
    override fun compare(a: ReflectKCallable<*>, b: ReflectKCallable<*>): Int {
        val typeParametersEliminator = a.typeParameters.substitutedWith(b.typeParameters)
            ?: error(
                "Intersection overrides can't have different type parameters sizes. " +
                        "It must have been reported by the compiler. " +
                        "The following members appear to be violating intersection overrides: '$a' '$b'"
            )
        val aReturnType = typeParametersEliminator.substituteTopLevelType(a.returnType, a.name)
        val bReturnType = b.returnType

        val aIsSubtypeOfB = aReturnType.isSubtypeOf(bReturnType)
        val bIsSubtypeOfA = bReturnType.isSubtypeOf(aReturnType)
        if (aIsSubtypeOfB && !bIsSubtypeOfA) return -1
        if (bIsSubtypeOfA && !aIsSubtypeOfB) return 1

        val isAFlexible = with(ReflectTypeSystemContext) { (aReturnType as? AbstractKType)?.isFlexible() == true }
        val isBFlexible = with(ReflectTypeSystemContext) { (bReturnType as? AbstractKType)?.isFlexible() == true }
        if (isBFlexible && !isAFlexible) return -1
        if (isAFlexible && !isBFlexible) return 1

        return 0
    }
}

internal typealias MembersJavaSignatureMap = Map<EquatableCallableSignature<EqualityMode.JavaSignature>, ReflectKCallable<*>>
private typealias MutableMembersJavaSignatureMap = MutableMap<EquatableCallableSignature<EqualityMode.JavaSignature>, ReflectKCallable<*>>
private typealias MutableMembersKotlinSignatureMap = MutableMap<EquatableCallableSignature<EqualityMode.KotlinSignature>, ReflectKCallable<*>>

private fun ReflectKCallable<*>.isStaticMethodInInterface(kClass: KClassImpl<*>): Boolean =
    isStatic && kClass.classKind == ClassKind.INTERFACE && !isJavaField

/**
 * Non-transitive members don't inherit transitively but appear in the 'members' list of the immediate KClass
 */
internal fun isNonTransitiveMember(kClass: KClassImpl<*>, member: ReflectKCallable<*>): Boolean =
    member.visibility == KVisibility.PRIVATE ||
            // static methods (but not fields) in interfaces are never inherited (neither in Java nor in Kotlin)
            member.isStaticMethodInInterface(kClass)

/**
 * Builds the "transitive" member map, for a single member [name], used to compute 'KClass.members' for every KClass.
 *
 * User facing 'KClass.members' is not "transitive". This map is "transitive".
 *
 * By "transitive" we mean that the map of every inheritor class/interface is a strict superset
 * of their parent classes' maps.
 */
internal fun computeFakeOverrideMembersForName(kClass: KClassImpl<*>, name: String): MembersJavaSignatureMap {
    val declaredMembers = kClass.data.value.getDeclaredMembersByName(name)
    val javaSignaturesMap: MutableMembersJavaSignatureMap = HashMap()
    val isKotlin = kClass.isKotlin
    val declaredTransitiveKotlinMembers: MutableMembersKotlinSignatureMap = HashMap()
    if (isKotlin) {
        for (member in declaredMembers) {
            if (isNonTransitiveMember(kClass, member)) continue
            declaredTransitiveKotlinMembers[member.toEquatableCallableSignature(EqualityMode.KotlinSignature)] = member
        }
    }
    for (supertype in kClass.supertypes) {
        val supertypeKClass = supertype.classifier as? KClassImpl<*>
            ?: error(
                "Non-denotable supertypes are not possible. " +
                        "Supertype '$supertype' appears non-denotable in class '$kClass'"
            )
        val substitutor = KTypeSubstitutor.create(supertype)
        val supertypeMembers = getSupertypeMembersByName(supertype, supertypeKClass, name) // Recursive call
        for (notSubstitutedMember in supertypeMembers) {
            val overriddenStorage = notSubstitutedMember.overriddenStorage
                .withChainedClassTypeParametersSubstitutor(substitutor)
                .copy(
                    isStatic = notSubstitutedMember.isStatic,
                    originalContainerIfFakeOverride = notSubstitutedMember.originalContainer,
                    originalCallableTypeParameters = notSubstitutedMember.typeParameters,
                    overridden = listOf(notSubstitutedMember),
                )
            val newMember = notSubstitutedMember.shallowCopy(kClass, overriddenStorage)
            val kotlinSignature = newMember.toEquatableCallableSignature(EqualityMode.KotlinSignature)
            if (declaredTransitiveKotlinMembers.contains(kotlinSignature)) continue
            // Inherited signatures are always compared by the JvmSignatures. Even for kotlin classes.
            val javaSignature = kotlinSignature.withEqualityMode(EqualityMode.JavaSignature)
            val existingMember = javaSignaturesMap[javaSignature]
            javaSignaturesMap[javaSignature] =
                if (existingMember == null) newMember
                else minOf(existingMember, newMember, CovariantOverrideComparator).let { result ->
                    if (existingMember is KFunction<*> && newMember is KFunction<*>)
                        result.shallowCopy(
                            result.container,
                            result.overriddenStorage.copy(
                                modality = minOf(existingMember, newMember, modalityIntersectionOverrideComparator).modality,
                                overridden = existingMember.overriddenStorage.overridden + newMember.overriddenStorage.overridden,
                                forceIsExternal = existingMember.isExternal || newMember.isExternal,
                                forceIsOperator = existingMember.isOperator || newMember.isOperator,
                                forceIsInfix = existingMember.isInfix || newMember.isInfix,
                                forceIsInline = existingMember.isInline || newMember.isInline,
                            ),
                        )
                    else result
                }
        }
    }
    for ((kotlinSignature, member) in declaredTransitiveKotlinMembers) {
        javaSignaturesMap[kotlinSignature.withEqualityMode(EqualityMode.JavaSignature)] = member
    }
    if (!isKotlin) {
        for (member in declaredMembers) {
            if (isNonTransitiveMember(kClass, member)) continue
            javaSignaturesMap[member.toEquatableCallableSignature(EqualityMode.JavaSignature)] = member
        }
    }
    return javaSignaturesMap
}

internal fun computeOverriddenFunctions(callable: ReflectKFunction): Collection<ReflectKFunction> {
    if (callable.overriddenStorage.isFakeOverride) {
        return callable.overriddenStorage.overridden.map { it as ReflectKFunction }
    }

    val container = callable.container as? KClassImpl<*> ?: return emptyList()
    val thisKotlinSignature = callable.toEquatableCallableSignature(EqualityMode.KotlinSignature)
    return computeOverriddenFunctions(container, thisKotlinSignature)
}

internal fun computeOverriddenFunctions(
    container: KClassImpl<*>,
    signature: EquatableCallableSignature<EqualityMode.KotlinSignature>,
): Collection<ReflectKFunction> {
    val result = mutableListOf<ReflectKFunction>()
    for (supertype in container.supertypes) {
        val supertypeKClass = supertype.classifier as? KClassImpl<*> ?: continue
        val substitutor = KTypeSubstitutor.create(supertype)
        for (notSubstitutedMember in getSupertypeMembersByName(supertype, supertypeKClass, signature.name)) {
            if (notSubstitutedMember !is ReflectKFunction) continue
            val overriddenStorage = notSubstitutedMember.overriddenStorage
                .withChainedClassTypeParametersSubstitutor(substitutor)
                .copy(
                    originalContainerIfFakeOverride = notSubstitutedMember.originalContainer,
                    originalCallableTypeParameters = notSubstitutedMember.typeParameters,
                    isStatic = notSubstitutedMember.isStatic,
                )
            val substitutedMember = notSubstitutedMember.shallowCopy(container, overriddenStorage)
            val memberKotlinSignature = substitutedMember.toEquatableCallableSignature(EqualityMode.KotlinSignature)
            if (signature == memberKotlinSignature) {
                result.add(notSubstitutedMember)
            }
        }
    }
    return result
}

private fun getSupertypeMembersByName(supertype: KType, supertypeKClass: KClassImpl<*>, name: String): Collection<ReflectKCallable<*>> {
    // There are no KClass instances for suspend function types before KT-79225, so we create suspend invoke manually.
    if (name == "invoke" && (supertype as? AbstractKType)?.isSuspendFunctionType == true) {
        val functionKmClass = supertypeKClass.kmClass
            ?: throw KotlinReflectionInternalError("No metadata found for function class '$supertypeKClass'")
        val invokeKmFunction = createSuspendFunctionInvoke(supertype.arguments.size - 1, functionKmClass)
        return listOf(createUnboundFunction(invokeKmFunction, supertypeKClass))
    }
    return supertypeKClass.getFakeOverrideMembersByName(name).values
}

private val modalityIntersectionOverrideComparator: Comparator<ReflectKCallable<*>> = compareBy(
    // Deprioritize interfaces, prioritize classes
    { (it.originalContainer as? KClass<*>)?.java?.isInterface == true },
    // If there are multiple superclasses (not interfaces), deprioritize kotlin.Any.
    // For instance, equals/hashCode/toString which come from interfaces have kotlin.Any container.
    { it.originalContainer == Any::class },
)

internal val ReflectKCallable<*>.originalContainer: KDeclarationContainerImpl
    get() = overriddenStorage.originalContainerIfFakeOverride ?: container

internal val ReflectKCallable<*>.isStatic: Boolean
    get() = overriddenStorage.isStatic ?: run {
        val parameters = (this as? JavaKNamedFunction)?.originalParameters ?: allParameters
        parameters.firstOrNull()?.kind != KParameter.Kind.INSTANCE
    }

private val ReflectKCallable<*>.isJavaField: Boolean
    get() = this is KProperty<*> && this.javaField?.declaringClass?.isKotlinClassOrPackage == false

internal fun <T : EqualityMode> ReflectKCallable<*>.toEquatableCallableSignature(equalityMode: T): EquatableCallableSignature<T> {
    val parameters = (this as? JavaKNamedFunction)?.originalParameters ?: allParameters
    val kotlinParameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when {
        isJavaField -> SignatureKind.FIELD_IN_JAVA_CLASS
        this is KProperty<*> -> SignatureKind.PROPERTY
        this is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    val isSuspend = (this as? KFunction<*>)?.isSuspend == true
    val functionJvmSignature = (this as? ReflectKFunction)?.signature
    val jvmNameIfFunction = functionJvmSignature?.substringBeforeLast('(')
    val functionJvmDescriptor = functionJvmSignature?.substring(jvmNameIfFunction!!.length)
    // JVM signature of suspend functions has a continuation parameter, which is absent in `kotlinParameterTypes`, so we drop it to keep
    // Java and Kotlin parameter lists aligned.
    val javaParameterTypes = functionJvmDescriptor?.let {
        container.jClass.safeClassLoader.parseAndLoadDescriptor(it, loadReturnType = false).parameters.dropContinuationIfSuspend(isSuspend)
    }.orEmpty()
    return EquatableCallableSignature(
        kind,
        name,
        jvmNameIfFunction,
        typeParameters,
        kotlinParameterTypes,
        javaParameterTypes,
        {
            // Workaround KT-13077: `javaMethod` doesn't work for builtins, so find the method manually, falling back to erased types.
            val method = when (this) {
                is JavaKNamedFunction -> jMethod
                is ReflectKFunction -> findOriginalJavaMethod(jvmNameIfFunction!!, javaParameterTypes)
                else -> null
            }
            method?.genericParameterTypes?.toList()?.dropContinuationIfSuspend(isSuspend) ?: javaParameterTypes
        },
        isSuspend,
        isStatic,
        equalityMode,
    )
}

// Returns the same method as `javaMethod`, unless the latter is a bridge method, in which case, for a fake override, returns the Java
// method of the original declaration in the class where the member is really declared. For example, for the fake override `invoke` in
// a Java class implementing `Function1<String, Integer>`, `javaMethod` returns the synthetic bridge `invoke(Object): Object` declared
// in the Java class, whose generic parameter types are erased, while this property returns `Function1.invoke(P1)` whose generic
// parameter type is the type variable `P1`.
private fun ReflectKFunction.findOriginalJavaMethod(name: String, parameterTypes: List<Class<*>>): Method? {
    val method = container.findMethodBySignature(name, parameterTypes, returnType = null) ?: return null
    if (!method.isBridge) return method
    val originalContainer = overriddenStorage.originalContainerIfFakeOverride ?: return method
    val jvmName = signature.substringBeforeLast('(')
    return originalContainer.findMethodBySignature(jvmName, signature.substring(jvmName.length)) ?: method
}

private fun <T> List<T>.dropContinuationIfSuspend(isSuspend: Boolean): List<T> = if (isSuspend) dropLast(1) else this

internal val Class<*>.isKotlinClassOrPackage: Boolean
    get() = getAnnotation(Metadata::class.java) != null

internal val KClassImpl<*>.isKotlin: Boolean
    get() = kmClass != null

internal fun List<KTypeParameter>.substitutedWith(arguments: List<KTypeParameter>): KTypeSubstitutor? {
    if (size != arguments.size) return null
    if (isEmpty()) return KTypeSubstitutor.EMPTY
    val substitutionMap = zip(arguments).associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
    return KTypeSubstitutor(substitutionMap)
}

internal enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

internal sealed class EqualityMode {
    /**
     * For declared members in Kotlin classes
     */
    data object KotlinSignature : EqualityMode()

    /**
     * For inherited members and declared members in Java classes; and for inherited members in Kotlin classes
     *
     * There is also the third kind of signatures: JVM signatures
     * JVM signature is a plain triple: (jvmName: String, parameters: List<Class<*>>, returnType: Class<*>)
     * Contrary to JVM signature, Java signature doesn't include `returnType`,
     * and Java signatures respect class generics (but not method generics)
     */
    data object JavaSignature : EqualityMode()
}

// Signatures that you can test for equality
internal class EquatableCallableSignature<T : EqualityMode>(
    val kind: SignatureKind,
    val name: String,
    val jvmNameIfFunction: String?,
    val typeParameters: List<KTypeParameter>,
    val kotlinParameterTypes: List<KType>,
    val javaErasedParameterTypes: List<Class<*>>,
    private val computeJavaGenericParameterTypes: () -> List<Type>,
    val isSuspend: Boolean,
    val isStatic: Boolean,
    val equalityMode: T,
) {
    private val javaGenericParameterTypes: List<Type> by lazy(PUBLICATION) {
        computeJavaGenericParameterTypes().also {
            check(it.size == javaErasedParameterTypes.size && javaErasedParameterTypes.size == kotlinParameterTypes.size) {
                "javaGenericParameterTypes.size (${it.size}), javaErasedParameterTypes.size (${javaErasedParameterTypes.size}) and " +
                        "kotlinParameterTypes.size (${kotlinParameterTypes.size}) must be equal. " +
                        "For member: '$name'"
            }
        }
    }

    private val isJavaFunctionSignature: Boolean
        get() = equalityMode == EqualityMode.JavaSignature && kind == SignatureKind.FUNCTION

    init {
        check(
            kind != SignatureKind.FIELD_IN_JAVA_CLASS ||
                    kotlinParameterTypes.isEmpty() && typeParameters.isEmpty() && javaErasedParameterTypes.isEmpty()
        ) {
            "Inconsistent combination of EquatableCallableSignature values. kind: ${kind}, " +
                    "kotlinParameterTypes.isEmpty(): ${kotlinParameterTypes.isEmpty()}," +
                    "typeParameters.isEmpty(): ${typeParameters.isEmpty()}, " +
                    "javaErasedParameterTypes.isEmpty(): ${javaErasedParameterTypes.isEmpty()}." +
                    "For member: '$name'"
        }
    }

    fun <T : EqualityMode> withEqualityMode(equalityMode: T): EquatableCallableSignature<T> =
        EquatableCallableSignature(
            kind,
            name,
            jvmNameIfFunction,
            typeParameters,
            kotlinParameterTypes,
            javaErasedParameterTypes,
            computeJavaGenericParameterTypes,
            isSuspend,
            isStatic,
            equalityMode
        )

    override fun hashCode(): Int =
        arrayOf<Any>(kind, kotlinParameterTypes.size, isStatic, if (isJavaFunctionSignature) jvmNameIfFunction ?: "" else name)
            .contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EquatableCallableSignature<*>) return false
        check(equalityMode == other.equalityMode) {
            "Equality modes must be the same for member '$name'. Please recreate signatures on inheritance"
        }
        if (kind != other.kind) return false
        if (isSuspend != other.isSuspend) return false
        if (isStatic != other.isStatic) return false
        if (kotlinParameterTypes.size != other.kotlinParameterTypes.size) return false
        return if (isJavaFunctionSignature) equalsByJavaSignature(other) else equalsByKotlinSignature(other)
    }

    private fun equalsByJavaSignature(other: EquatableCallableSignature<*>): Boolean =
        jvmNameIfFunction == other.jvmNameIfFunction &&
                javaErasedParameterTypes.indices.all { i -> areEqualJavaParameterTypes(i, other) }

    private fun areEqualJavaParameterTypes(i: Int, other: EquatableCallableSignature<*>): Boolean =
        if (javaGenericParameterTypes[i].isClassTypeParameter || other.javaGenericParameterTypes[i].isClassTypeParameter) {
            javaErasedParameterTypes[i].isPrimitive == other.javaErasedParameterTypes[i].isPrimitive &&
                    // Since we don't have type substitutors for Java types, here we abuse KTypes for this purpose
                    areEqualKTypes(kotlinParameterTypes[i], other.kotlinParameterTypes[i])
        } else {
            javaErasedParameterTypes[i] == other.javaErasedParameterTypes[i]
        }

    private fun equalsByKotlinSignature(other: EquatableCallableSignature<*>): Boolean {
        if (name != other.name) return false
        val functionTypeParametersEliminator = typeParameters.substitutedWith(other.typeParameters) ?: return false
        if (!areEqualTypeParameterBounds(functionTypeParametersEliminator, other)) return false
        for (i in kotlinParameterTypes.indices) {
            val a = functionTypeParametersEliminator.substituteTopLevelType(kotlinParameterTypes[i], name)
            val b = other.kotlinParameterTypes[i]
            if (!areEqualKTypes(a, b)) return false
        }
        return true
    }

    private fun areEqualTypeParameterBounds(substitutor: KTypeSubstitutor, other: EquatableCallableSignature<*>): Boolean {
        for (i in typeParameters.indices) {
            val typeParameterA = typeParameters[i]
            val typeParameterB = other.typeParameters[i]
            if (typeParameterA.upperBounds.size != typeParameterB.upperBounds.size) return false
            val equalUpperBounds = typeParameterA.upperBounds
                .map { substitutor.substituteTopLevelType(it, name) }
                .sortedUpperBounds(memberNameForDebug = name)
                .zip(typeParameterB.upperBounds.sortedUpperBounds(memberNameForDebug = other.name))
                .all { areEqualKTypes(it.first, it.second) }
            if (!equalUpperBounds) return false
        }
        return true
    }
}

private val Type.isClassTypeParameter: Boolean
    get() = this is TypeVariable<*> && genericDeclaration is Class<*>

/**
 * Those upper bounds are already substituted, so equal lists of upper bounds must also have equal names.
 * The necessary condition for equal upper bounds is equal names.
 *
 * The only false negative case that we are afraid of is when different upper bounds accidentally have the same name.
 * In that case, the list of bounds will be discarded later by areEqualTypes anyway.
 */
private fun List<KType>.sortedUpperBounds(memberNameForDebug: String): List<KType> =
    sortedBy {
        when (
            val classifier = it.classifier ?: error(
                "Upper bounds are always denotable. " +
                        "Upper bounds appear non-denotable for member: '$memberNameForDebug'"
            )
        ) {
            is KClass<*> -> classifier.java.name
            is KTypeParameter -> classifier.name
            else -> error("Unknown upper bound classifier: ${classifier::class}")
        }
    }
