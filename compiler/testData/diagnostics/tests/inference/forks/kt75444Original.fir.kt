// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75444

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface KeyType {
    object Integrated : KeyType
}

sealed interface NonceTrait {
    class Required : NonceTrait
    object Without : NonceTrait
}

sealed interface AuthCapability<K : KeyType> {
    sealed class Authenticated<K : KeyType> : AuthCapability<K>

    object Unauthenticated : AuthCapability<KeyType.Integrated>
}


sealed interface Algorithm<out A : AuthCapability<out K>, out I : NonceTrait, out K : KeyType> {
    sealed interface Unauthenticated<out I : NonceTrait> :
        Algorithm<AuthCapability.Unauthenticated, I, KeyType.Integrated>

    sealed interface Authenticated<out A : AuthCapability.Authenticated<out K>, out I : NonceTrait, out K : KeyType> :
        Algorithm<A, I, K>

    sealed interface RequiringNonce<out A : AuthCapability<out K>, K : KeyType> :
        Algorithm<A, NonceTrait.Required, K>

    sealed interface WithoutNonce<out A : AuthCapability<out K>, K : KeyType> :
        Algorithm<A, NonceTrait.Without, K>
}

@OptIn(ExperimentalContracts::class)
fun <I : NonceTrait, K : KeyType> Algorithm<*, I, K>.isAuthenticated(): Boolean {
    contract {
        returns(true) implies (this@isAuthenticated is Algorithm.Authenticated<*, I, K>)
        returns(false) implies (this@isAuthenticated is Algorithm.Unauthenticated<I>)
    }
    TODO()
}

@OptIn(ExperimentalContracts::class)
fun <A : AuthCapability<out K>, K : KeyType> Algorithm<A, *, K>.requiresNonce(): Boolean {
    contract {
        returns(true) implies (this@requiresNonce is Algorithm.RequiringNonce<A, K>)
        returns(false) implies (this@requiresNonce is Algorithm.WithoutNonce<A, K>)
    }
    TODO()
}

fun<A: AuthCapability.Authenticated<out K>, K: KeyType> Algorithm<A, NonceTrait.Required, K>.foo() {}

fun test_1_1(algorithm: Algorithm<AuthCapability<out KeyType>, NonceTrait, KeyType>) {
    algorithm.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // wrong receiver
}

fun test_1_2(algorithm: Algorithm<AuthCapability<out KeyType>, NonceTrait, KeyType>) {
    if (!algorithm.requiresNonce()) {
        algorithm.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // wrong receiver
    }
}

fun test_1_3(algorithm: Algorithm<AuthCapability<out KeyType>, NonceTrait, KeyType>) {
    if (algorithm.isAuthenticated()) {
        algorithm.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // wrong receiver
    }
}

fun test_1_4(algorithm: Algorithm<AuthCapability<out KeyType>, NonceTrait, KeyType>) {
    if (!algorithm.requiresNonce() && algorithm.isAuthenticated()) {
        algorithm.foo() // OK, but should be wrong receiver
    }
}

fun test_2_1(algorithm: Algorithm.WithoutNonce<AuthCapability<out KeyType>, KeyType>) {
    algorithm.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // wrong receiver
}


fun test_2_2(algorithm: Algorithm.Authenticated<*, NonceTrait.Without, KeyType>) {
    algorithm.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>() // wrong receiver
}
