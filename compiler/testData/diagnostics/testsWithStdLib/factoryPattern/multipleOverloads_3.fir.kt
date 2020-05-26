// !LANGUAGE: +NewInference +FactoryPatternResolution
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION -EXPERIMENTAL_API_USAGE -EXPERIMENTAL_UNSIGNED_LITERALS
// ISSUE: KT-11265

// FILE: OverloadResolutionByLambdaReturnType.kt

package kotlin

annotation class OverloadResolutionByLambdaReturnType

// FILE: main.kt

import kotlin.OverloadResolutionByLambdaReturnType

public inline fun <T, R> Iterable<T>.myFlatMap(transform: (T) -> Iterable<R>): List<R> {
    TODO()
}

@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("myFlatMapSequence")
public inline fun <T, R> Iterable<T>.myFlatMap(transform: (T) -> Sequence<R>): List<R> {
    TODO()
}

interface Name
interface DeclarationDescriptor {
    val nextCandidates: List<DeclarationDescriptor>?
    val nextCandidatesSeq: Sequence<DeclarationDescriptor>?
    val name: Name
}

fun test_1(name: Name, toplevelDescriptors: List<DeclarationDescriptor>): List<DeclarationDescriptor> {
    val candidates = toplevelDescriptors.<!AMBIGUITY!>myFlatMap<!> { container ->
        val nextCandidates = container.<!UNRESOLVED_REFERENCE!>nextCandidates<!> ?: return@myFlatMap emptyList()
        nextCandidates
    }
    return candidates
}

fun test_2(name: Name, toplevelDescriptors: List<DeclarationDescriptor>): List<DeclarationDescriptor> {
    val candidates = toplevelDescriptors.<!AMBIGUITY!>myFlatMap<!> { container ->
        val nextCandidates = container.<!UNRESOLVED_REFERENCE!>nextCandidatesSeq<!> ?: return@myFlatMap sequenceOf()
        nextCandidates
    }
    return candidates
}

fun test_3(name: Name, toplevelDescriptors: List<DeclarationDescriptor>): List<DeclarationDescriptor> {
    val candidates = toplevelDescriptors.<!AMBIGUITY!>myFlatMap<!> { container ->
        val nextCandidates = container.<!UNRESOLVED_REFERENCE!>nextCandidatesSeq<!>!!
        nextCandidates
    }
    return candidates
}
