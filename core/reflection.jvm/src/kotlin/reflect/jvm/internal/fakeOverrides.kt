/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

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

/**
 * Unfortunately, getting members is not a transitive operation
 */
internal fun getAllMembers_newKotlinReflect(kClass: KClassImpl<*>): Collection<DescriptorKCallable<*>> {
    val membersReadOnlyList = kClass.data.value.allMembersPreservingTransitivity
    // Kotlin doesn't have statics (unless it's enum), and it never inherits statics from Java
    val doNeedToFilterOutStatics =
        membersReadOnlyList.containsInheritedStatics && kClass.classKind != ClassKind.ENUM_CLASS && kClass.java.isKotlin
    val doNeedToShrinkMembers = membersReadOnlyList.containsPackagePrivate || doNeedToFilterOutStatics
    val membersMutableList = when (doNeedToShrinkMembers) {
        true -> lazyOf(
            membersReadOnlyList.allMembers.filterNotTo(HashMap(membersReadOnlyList.allMembers.size)) { (_, member) ->
                doNeedToFilterOutStatics && member.isStatic ||
                    member.isPackagePrivate && member.container.jClass.`package` != kClass.java.`package`
            }
        )
        false -> lazy(LazyThreadSafetyMode.NONE) { HashMap(membersReadOnlyList.allMembers) }
    }
    for (declaredMember in kClass.declaredDescriptorKCallableMembers) {
        val signature = declaredMember.toEquatableCallableSignature()
        // static members are not inherited, but the immediate statics in interfaces must appear in the 'members' list
        if (declaredMember.isStatic && kClass.classKind == ClassKind.INTERFACE) {
            membersMutableList.value[signature] = declaredMember
        }
        // private members are not inherited, but immediate private members must appear in the 'members' list
        if (declaredMember.visibility == KVisibility.PRIVATE) membersMutableList.value[signature] = declaredMember
    }
    return (if (membersMutableList.isInitialized()) membersMutableList.value else membersReadOnlyList.allMembers)
        .map { it.value }
}

private fun nonDenotableSupertypesAreNotPossible(): Nothing = error("Non-denotable supertypes are not possible")
private fun starProjectionSupertypesAreNotPossible(): Nothing = error("Star projection supertypes are not possible")

// The Comparator assumes similar signature KCallables
private object CovariantOverrideComparator : Comparator<DescriptorKCallable<*>> {
    override fun compare(a: DescriptorKCallable<*>, b: DescriptorKCallable<*>): Int {
        val typeParametersEliminator = b.typeParameters.substituteTypeParametersInto(a.typeParameters)
        val aReturnType = typeParametersEliminator.substitute(a.returnType).type ?: starProjectionSupertypesAreNotPossible()
        val bReturnType = b.returnType
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
            else -> 0 //error("RETURN_TYPE_MISMATCH_ON_INHERITANCE") // todo
        }
    }
}

private val collectionKType = typeOf<Collection<*>>()

internal data class AllMembersPreservingTransitivity(
    val allMembers: Map<EquatableCallableSignature, DescriptorKCallable<*>>,
    val containsInheritedStatics: Boolean,
    val containsPackagePrivate: Boolean,
)

/**
 * todo KDoc
 */
internal fun getAllMembersPreservingTransitivity(kClass: KClassImpl<*>): AllMembersPreservingTransitivity {
    val outVisitedSignatures = HashMap<EquatableCallableSignature, DescriptorKCallable<*>>()
    val thisReceiver = kClass.descriptor.thisAsReceiverParameter
    var containsInheritedStatics = false
    var containsPackagePrivate = false
    for (rawSupertype in kClass.supertypes) {
        val supertype = (rawSupertype as? AbstractKType)?.let {
            // It's a hack for Flexible Collection types.
            // They don't have proper KClasses KT-11754
            // which leads to type substitution quirks (because we have proper KTypes for them)
            when (it.isSubtypeOf(collectionKType)) {
                true ->
                    it.upperBoundIfFlexible()
                false ->
                    it//.lowerBoundIfFlexible() // todo formatting
            }
        } ?: rawSupertype
        val supertypeKClass = (supertype as AbstractKType).correctClassifier as? KClass<*> ?: nonDenotableSupertypesAreNotPossible()
        val substitutor = KTypeSubstitutor.create(supertype)
        val supertypeMembers = supertypeKClass.allMembersPreservingTransitivity
        containsInheritedStatics = containsInheritedStatics || supertypeMembers.containsInheritedStatics
        containsPackagePrivate = containsPackagePrivate || supertypeMembers.containsPackagePrivate
        for ((_, notSubstitutedMember) in supertypeMembers.allMembers) {
            val isStaticMember = notSubstitutedMember.instanceReceiverParameter == null
            val member = notSubstitutedMember.shallowCopy().apply {
                forceInstanceReceiverParameter = if (isStaticMember) null else thisReceiver
                kTypeSubstitutor =
                    (notSubstitutedMember.kTypeSubstitutor ?: KTypeSubstitutor.EMPTY).combinedWith(substitutor)
            }
            outVisitedSignatures.merge(member.toEquatableCallableSignature(), member) { a, b ->
                minOf(a, b, CovariantOverrideComparator)
            }
        }
    }
    for (member in kClass.declaredDescriptorKCallableMembers) {
        if (member.visibility == KVisibility.PRIVATE) continue
        val isStaticMember = member.instanceReceiverParameter == null
        // static members in interfaces are never inherited (not in Java, not in Kotlin enums)
        if (isStaticMember && kClass.classKind == ClassKind.INTERFACE) continue
        containsInheritedStatics = containsInheritedStatics || isStaticMember
        containsPackagePrivate = containsPackagePrivate || member.isPackagePrivate
        outVisitedSignatures[member.toEquatableCallableSignature()] = member
    }
    return AllMembersPreservingTransitivity(outVisitedSignatures, containsInheritedStatics, containsPackagePrivate)
}

private val DescriptorKCallable<*>.isStatic: Boolean
    get() = instanceReceiverParameter == null

private val KClass<*>.allMembersPreservingTransitivity: AllMembersPreservingTransitivity
    get() = when (this) {
        is KClassImpl<*> -> data.value.allMembersPreservingTransitivity
        is MutableCollectionKClass<*> -> klass.allMembersPreservingTransitivity
        else -> error("Unknown type ${this::class}")
    }

private val KClass<*>.classKind: ClassKind // todo do I need it?
    get() = when (this) {
        is KClassImpl<*> -> classKind
        is MutableCollectionKClass<*> -> klass.classKind
        else -> error("Unknown type ${this::class}")
    }

private fun DescriptorKCallable<*>.toEquatableCallableSignature(): EquatableCallableSignature {
    val parameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when (this) {
        is KProperty<*> if javaField?.declaringClass?.isKotlin == false -> SignatureKind.FIELD_IN_JAVA_CLASS
        is KProperty<*> -> SignatureKind.PROPERTY
        is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    return EquatableCallableSignature(kind, name, this.typeParameters, parameterTypes, isStatic)
}

internal enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

internal val Class<*>.isKotlin: Boolean get() = getAnnotation(Metadata::class.java) != null

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredDescriptorKCallableMembers: Collection<DescriptorKCallable<*>>
    get() = declaredMembers as Collection<DescriptorKCallable<*>>

private fun List<KTypeParameter>.substituteTypeParametersInto(typeParameters: List<KTypeParameter>): KTypeSubstitutor {
    if (isEmpty() || typeParameters.isEmpty()) return KTypeSubstitutor.EMPTY
    val arguments = this
    val substitutionMap = typeParameters.zip(arguments)
        .associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
    return KTypeSubstitutor(substitutionMap)
}

// Signatures that you can test for equality
internal class EquatableCallableSignature(
    val kind: SignatureKind,
    val name: String,
    val typeParameters: List<KTypeParameter>,
    val parameterTypes: List<KType>,
    val isStatic: Boolean,
) {
    init {
        check(kind != SignatureKind.FIELD_IN_JAVA_CLASS || parameterTypes.isEmpty() && typeParameters.isEmpty())
    }

    override fun hashCode(): Int = Objects.hash(kind, name, typeParameters.size, parameterTypes.size, isStatic)

    override fun equals(other: Any?): Boolean {
        if (other !is EquatableCallableSignature) return false
        if (kind != other.kind) return false
        if (name != other.name) return false
        if (isStatic != other.isStatic) return false
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
