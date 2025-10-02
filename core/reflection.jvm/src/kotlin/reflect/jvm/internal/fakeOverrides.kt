/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import java.util.*
import kotlin.metadata.ClassKind
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor
import kotlin.reflect.jvm.javaField

internal fun getAllMembers_newKotlinReflectImpl(
    kClass: KClassImpl<*>,
    memberKind: MemberKind,
): List<DescriptorKCallable<*>> {
    val isKotlin = kClass.jClass.isKotlin
    // Kotlin doesn't have statics (unless it's enum), and it never inherits statics from Java
    if (kClass.classKind != ClassKind.ENUM_CLASS && memberKind == MemberKind.STATIC && isKotlin) return emptyList()
    val out = ArrayList<DescriptorKCallable<*>>()
    val visitedSignatures = HashSet<EquatableCallableSignature>()
    for (member in kClass.declaredDescriptorKCallableMembers) {
        val isStaticMember = member.instanceReceiverParameter == null
        if (isStaticMember != (memberKind == MemberKind.STATIC)) continue
        out.add(member)
    }
    val declaredMembersSignatures =
        kClass.declaredDescriptorKCallableMembers.mapTo(HashSet()) { it.toCallableSignature(KTypeSubstitutor.EMPTY) }
    check(declaredMembersSignatures.size == kClass.declaredDescriptorKCallableMembers.size) // todo drop check?
    visitedSignatures.addAll(declaredMembersSignatures)
    val visitedClassifiers = HashSet<KClass<*>>()
    visitedClassifiers.add(kClass)
    val supertypes = kClass.supertypes
    val receiver = kClass.descriptor.thisAsReceiverParameter
    for (supertype in supertypes) {
        val context = RecursionContext(receiver, visitedClassifiers, visitedSignatures, isKotlin, memberKind)
        val members = getSuperMembersRecursive(supertype, KTypeSubstitutor.EMPTY, context)
        out.addAll(members)
    }
    return out
}

internal enum class MemberKind {
    STATIC, INNER
}

private fun nonDenotableSupertypesAreNotPossible(): Nothing = error("Non-denotable supertypes are not possible")
private fun starProjectionSupertypesAreNotPossible(): Nothing = error("Star projection supertypes are not possible")

private data class RecursionContext(
    val receiver: ReceiverParameterDescriptor,
    val visitedClassifiers: HashSet<KClass<*>>,
    val visitedSignatures: MutableSet<EquatableCallableSignature>,
    val isReceiverKotlin: Boolean,
    val memberKind: MemberKind,
)

private fun getSuperMembersRecursive(
    currentType: KType,
    accumulateSubstitutor: KTypeSubstitutor,
    context: RecursionContext,
): List<DescriptorKCallable<*>> = buildList {
    val currentClass = currentType.classifier as? KClass<*> ?: nonDenotableSupertypesAreNotPossible()
    if (!context.visitedClassifiers.add(currentClass)) return emptyList()
    val substitutor =
        accumulateSubstitutor.createCombinedSubstitutorOrNull(currentType) ?: starProjectionSupertypesAreNotPossible()
    for (member in currentClass.declaredDescriptorKCallableMembers) {
        if (member.visibility == KVisibility.PRIVATE) continue
        val isStaticMember = member.instanceReceiverParameter == null
        // static members in interfaces are never inherited (not in Java, not in Kotlin enums)
        if (isStaticMember && (currentClass as KClassImpl<*>).classKind == ClassKind.INTERFACE) continue
        if (isStaticMember != (context.memberKind == MemberKind.STATIC)) continue
        val signature = member.toCallableSignature(substitutor)
        if (!context.visitedSignatures.add(signature)) continue
        val substitutedMember = member.shallowCopy().apply {
            forceInstanceReceiverParameter = if (isStaticMember) null else context.receiver
            kTypeSubstitutor = substitutor
        }
        add(substitutedMember)
    }
    for (supertype in currentClass.supertypes) {
        addAll(getSuperMembersRecursive(supertype, substitutor, context))
    }
}

private fun KCallable<*>.toCallableSignature(substitutor: KTypeSubstitutor): EquatableCallableSignature {
    val parameterTypes = parameters
        .filter { it.kind != KParameter.Kind.INSTANCE }
        .map {
            // KTypeSubstitutor(mapOf())
            substitutor.substitute(it.type).type ?: starProjectionSupertypesAreNotPossible()
        }
    val kind = when (this) {
        is KProperty<*> if javaField?.declaringClass?.isKotlin == false -> SignatureKind.FIELD_IN_JAVA_CLASS
        is KProperty<*> -> SignatureKind.PROPERTY
        is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    val typeParameters = typeParameters
    // with(ReflectTypeSystemContext) {
    //     typeParameters.first().starProjectedType
    // }
    // KTypeSubstitutor(mapOf())
    return EquatableCallableSignature(kind, name, typeParameters, parameterTypes)
}

private enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

private val Class<*>.isKotlin: Boolean get() = getAnnotation(Metadata::class.java) != null

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredDescriptorKCallableMembers: Collection<DescriptorKCallable<*>>
    get() = declaredMembers as Collection<DescriptorKCallable<*>>

// Signatures that you can test for equality
private data class EquatableCallableSignature(
    val kind: SignatureKind,
    val name: String,
    val typeParameters: List<KTypeParameter>,
    val parameterTypes: List<KType>,
) {
    init {
        check(kind != SignatureKind.FIELD_IN_JAVA_CLASS || parameterTypes.isEmpty())
    }

    override fun hashCode(): Int = Objects.hash(kind, name, typeParameters.size)

    override fun equals(other: Any?): Boolean {
        if (other !is EquatableCallableSignature) return false
        if (kind != other.kind) return false
        if (name != other.name) return false
        if (typeParameters.size != other.typeParameters.size) return false
        if (parameterTypes.size != other.parameterTypes.size) return false
        val substitution = typeParameters.zip(other.typeParameters)
            .associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
        val typeParametersEliminator = KTypeSubstitutor(substitution)
        for (i in parameterTypes.indices) {
            val x = typeParametersEliminator.substitute(parameterTypes[i]).type
                ?: starProjectionSupertypesAreNotPossible()
            val y = other.parameterTypes[i]
            if (!x.isSubtypeOf(y) || !y.isSubtypeOf(x)) return false
        }
        return true
    }
}
