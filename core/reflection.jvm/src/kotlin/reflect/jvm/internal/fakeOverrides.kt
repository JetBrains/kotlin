/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.types.model.isFlexible
import java.util.*
import kotlin.metadata.ClassKind
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext
import kotlin.reflect.jvm.javaField

internal fun getAllMembers_newKotlinReflectImpl(
    kClass: KClassImpl<*>,
    memberKind: MemberKind,
): List<DescriptorKCallable<*>> {
    // return emptyList()
    val isKotlin = kClass.java.isKotlin
    // Kotlin doesn't have statics (unless it's enum), and it never inherits statics from Java
    if (kClass.classKind != ClassKind.ENUM_CLASS && memberKind == MemberKind.STATIC && isKotlin) return emptyList()
    val visitedSignatures = kClass.declaredDescriptorKCallableMembers
        .filter {
            val isStaticMember = it.instanceReceiverParameter == null
            isStaticMember == (memberKind == MemberKind.STATIC)
        }
        .associateTo(HashMap()) {
            Pair(it.toEquatableCallableSignature(), TopologicalKCallable(topologicalLevel = PartiallyOrderedNumber.zero, it))
        }
    val visitedClassifiers = HashSet<KClass<*>>()
    visitedClassifiers.add(kClass)
    val supertypes = kClass.supertypes
    val receiver = kClass.descriptor.thisAsReceiverParameter
    for (supertype in supertypes) {
        val context = RecursionContext(kClass, receiver, visitedClassifiers, memberKind)
        collectVisitedSignaturesForSuperclassRecursively(
            supertype,
            KTypeSubstitutor.EMPTY,
            PartiallyOrderedNumber.zero,
            context,
            visitedSignatures
        )
    }
    return visitedSignatures.map { it.value.callable }
}

internal enum class MemberKind {
    STATIC, INNER
}

private fun nonDenotableSupertypesAreNotPossible(): Nothing = error("Non-denotable supertypes are not possible")
private fun starProjectionSupertypesAreNotPossible(): Nothing = error("Star projection supertypes are not possible")

private data class RecursionContext(
    val receiver: KClassImpl<*>,
    val receiverParameterDescriptor: ReceiverParameterDescriptor,
    val visitedClassifiers: HashSet<KClass<*>>,
    val memberKind: MemberKind,
)

private class PartiallyOrderedNumber private constructor(val prev: PartiallyOrderedNumber?) {
    fun compareToOrNullIfNotComparable(other: PartiallyOrderedNumber): Int? = when {
        this == other -> 0
        this.isStricklyBiggerThan(other) -> 1
        other.isStricklyBiggerThan(this) -> -1
        else -> null // not comparable
    }

    fun isStricklyBiggerThan(other: PartiallyOrderedNumber): Boolean {
        var current = this
        while (true) {
            current = current.prev ?: return false
            if (current == other) return true
        }
    }

    fun createBiggerNumber(): PartiallyOrderedNumber = PartiallyOrderedNumber(this)

    companion object {
        val zero = PartiallyOrderedNumber(null)
    }
}

// The Comparator assumes similar signature KCallables
private object CovariantOverrideComparator : Comparator<TopologicalKCallable<*>> { // todo IntersectionOverrideComparator?
    override fun compare(a: TopologicalKCallable<*>, b: TopologicalKCallable<*>): Int {
        a.topologicalLevel.compareToOrNullIfNotComparable(b.topologicalLevel)?.let {
            check(it != 0) { "CONFLICTING_OVERLOADS" }
            return it
        }
        val typeParametersEliminator = b.callable.typeParameters.substituteTypeParametersInto(a.callable.typeParameters)
        val aReturnType =
            typeParametersEliminator.substitute(a.callable.returnType).type ?: starProjectionSupertypesAreNotPossible()
        val bReturnType = b.callable.returnType
        val aIsSubtypeOfB = aReturnType.isSubtypeOf(bReturnType)
        val bIsSubtypeOfA = bReturnType.isSubtypeOf(aReturnType)

        return when {
            aIsSubtypeOfB && bIsSubtypeOfA -> {
                val isAFlexible = with(ReflectTypeSystemContext) { (aReturnType as? AbstractKType)?.isFlexible() == true }
                val isBFlexible = with(ReflectTypeSystemContext) { (bReturnType as? AbstractKType)?.isFlexible() == true }
                when {
                    isAFlexible && isBFlexible -> 0
                    isBFlexible -> -1
                    isAFlexible -> 1
                    else -> 0
                }
            }
            aIsSubtypeOfB -> -1
            bIsSubtypeOfA -> 1
            else -> error("RETURN_TYPE_MISMATCH_ON_INHERITANCE")
        }
    }
}

private data class TopologicalKCallable<out T>(
    // The closer to the 'Any' type the member is, the bigger the number is
    val topologicalLevel: PartiallyOrderedNumber,
    val callable: DescriptorKCallable<T>,
)

private val collectionKType = typeOf<Collection<*>>()

private fun collectVisitedSignaturesForSuperclassRecursively(
    rawCurrentType: KType,
    accumulateClassGenericsSubstitutor: KTypeSubstitutor,
    prevTopologicalLevel: PartiallyOrderedNumber,
    context: RecursionContext,

    outVisitedSignatures: MutableMap<EquatableCallableSignature, TopologicalKCallable<*>>,
) {
    val currentType = (rawCurrentType as? AbstractKType)?.let {
        // It's a hack for Flexible Collection types.
        // They don't have proper KClasses KT-11754
        // which leads to type substitution quirks (because we have proper KTypes for them)
        when (it.isSubtypeOf(collectionKType)) {
            true ->
                it.upperBoundIfFlexible()
            else ->
                it//.lowerBoundIfFlexible() // todo formatting
        }
    } ?: rawCurrentType

    val currentClass = (currentType as AbstractKType).correctClassifier as? KClass<*>
        ?: nonDenotableSupertypesAreNotPossible()
    val topologicalLevel = prevTopologicalLevel.createBiggerNumber()
    if (!context.visitedClassifiers.add(currentClass)) return
    val classGenericsSubstitutor = accumulateClassGenericsSubstitutor.createCombinedSubstitutorOrNull(currentType)
        ?: starProjectionSupertypesAreNotPossible()
    for (notSubstitutedMember in currentClass.declaredDescriptorKCallableMembers) {
        // if (notSubstitutedMember.name != "containsAll" && notSubstitutedMember.name != "addAll") continue // todo debug
        if (notSubstitutedMember.visibility == KVisibility.PRIVATE) continue
        if (notSubstitutedMember.fullVisibility == JavaDescriptorVisibilities.PACKAGE_VISIBILITY &&
            currentClass.java.`package` != context.receiver.java.`package`
        ) {
            continue
        }
        val isStaticMember = notSubstitutedMember.instanceReceiverParameter == null
        // static members in interfaces are never inherited (not in Java, not in Kotlin enums)
        if (isStaticMember && currentClass.classKind == ClassKind.INTERFACE) continue
        if (isStaticMember != (context.memberKind == MemberKind.STATIC)) continue

        val member = notSubstitutedMember.shallowCopy().apply {
            forceInstanceReceiverParameter = if (isStaticMember) null else context.receiverParameterDescriptor
            kTypeSubstitutor = classGenericsSubstitutor
        }

        // Unit // todo
        //
        // val foo = member.shallowCopy().apply {
        //     forceInstanceReceiverParameter = member.forceInstanceReceiverParameter
        //     kTypeSubstitutor = member.kTypeSubstitutor
        // }
        // println(foo.toString())

        val signature = member.toEquatableCallableSignature()
        val topologicalMember = TopologicalKCallable(topologicalLevel, member)
        val existingMember = outVisitedSignatures[signature]

        if (existingMember == null || CovariantOverrideComparator.compare(topologicalMember, existingMember) < 0) {
            outVisitedSignatures[signature] = topologicalMember
        }
    }
    for (supertype in currentClass.supertypes) {
        collectVisitedSignaturesForSuperclassRecursively(
            supertype,
            classGenericsSubstitutor,
            topologicalLevel,
            context,
            outVisitedSignatures
        )
    }
}

private val KClass<*>.classKind: ClassKind // todo do I need it?
    get() {
        return when (this) {
            is KClassImpl<*> -> classKind
            is MutableCollectionKClass<*> -> klass.classKind
            else -> error("Unknown type ${this::class}")
        }
    }

private fun KCallable<*>.toEquatableCallableSignature(): EquatableCallableSignature {
    val parameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when (this) {
        is KProperty<*> if javaField?.declaringClass?.isKotlin == false -> SignatureKind.FIELD_IN_JAVA_CLASS
        is KProperty<*> -> SignatureKind.PROPERTY
        is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    return EquatableCallableSignature(kind, name, this.typeParameters, parameterTypes)
}

private enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

private val Class<*>.isKotlin: Boolean get() = getAnnotation(Metadata::class.java) != null

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredDescriptorKCallableMembers: Collection<DescriptorKCallable<*>>
    get() = declaredMembers as Collection<DescriptorKCallable<*>>

private fun List<KTypeParameter>.substituteTypeParametersInto(typeParameters: List<KTypeParameter>): KTypeSubstitutor {
    val arguments = this
    val substitutionMap = typeParameters.zip(arguments)
        .associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
    return KTypeSubstitutor(substitutionMap)
}

// Signatures that you can test for equality
private data class EquatableCallableSignature(
    val kind: SignatureKind,
    val name: String,
    val typeParameters: List<KTypeParameter>,
    val parameterTypes: List<KType>,
) {
    init {
        check(kind != SignatureKind.FIELD_IN_JAVA_CLASS || parameterTypes.isEmpty() && typeParameters.isEmpty())
    }

    override fun hashCode(): Int = Objects.hash(kind, name, typeParameters.size, parameterTypes.size)

    override fun equals(other: Any?): Boolean {
        if (other !is EquatableCallableSignature) return false
        if (kind != other.kind) return false
        if (name != other.name) return false
        if (typeParameters.size != other.typeParameters.size) return false
        if (parameterTypes.size != other.parameterTypes.size) return false
        val functionTypeParametersEliminator = other.typeParameters.substituteTypeParametersInto(typeParameters)
        for (i in parameterTypes.indices) {
            val x = functionTypeParametersEliminator.substitute(parameterTypes[i]).type
                ?: starProjectionSupertypesAreNotPossible()
            val y = other.parameterTypes[i]
            if (!x.isSubtypeOf(y) || !y.isSubtypeOf(x)) return false
        }
        return true
    }
}
