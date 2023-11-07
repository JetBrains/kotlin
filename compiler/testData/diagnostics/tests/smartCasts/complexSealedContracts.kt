// !OPT_IN: kotlin.contracts.ExperimentalContracts
// WITH_STDLIB
// ISSUE: KT-51160

import kotlin.contracts.*

sealed interface AsyncStatus<out T : Any> {
    val value: T?

    data class Pending<out T : Any>(
        override val value: T? = null,
    ) : AsyncStatus<T>

    sealed interface Terminal<out T : Any> : AsyncStatus<T>

    data class Error<out T : Any>(
        val error: Throwable,
        override val value: T? = null,
    ) : Terminal<T>

    data class Success<out T : Any>(
        override val value: T,
    ) : Terminal<T>
}

fun <T : Any> AsyncStatus<T>.isPending(): Boolean {
    contract {
        returns(true) implies (this@isPending is AsyncStatus.Pending)
        returns(false) implies (this@isPending is AsyncStatus.Terminal)
    }
    return (this is AsyncStatus.Pending)
}

fun <T : Any> AsyncStatus<T>.isSuccess(): Boolean {
    contract { returns(true) implies (this@isSuccess is AsyncStatus.Success) }
    return (this is AsyncStatus.Success)
}

fun <T : Any> AsyncStatus.Terminal<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is AsyncStatus.Success)
        returns(false) implies (this@isSuccess is AsyncStatus.Error)
    }
    return (this is AsyncStatus.Success)
}

fun <T : Any> AsyncStatus<T>.isError(): Boolean {
    contract { returns(true) implies (this@isError is AsyncStatus.Error) }
    return (this is AsyncStatus.Error)
}

fun <T : Any> AsyncStatus.Terminal<T>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is AsyncStatus.Error)
        returns(false) implies (this@isError is AsyncStatus.Success)
    }
    return (this is AsyncStatus.Error)
}

fun <T : Any> AsyncStatus<T>.isTerminal(): Boolean {
    contract {
        returns(true) implies (this@isTerminal is AsyncStatus.Terminal)
        returns(false) implies (this@isTerminal is AsyncStatus.Pending)
    }
    return (this is AsyncStatus.Terminal)
}

fun main() {
    val foo = AsyncStatus.Pending<String>()
    if (!foo.isPending()) {
        if (<!DEBUG_INFO_SMARTCAST!>foo<!>.isSuccess()) {
            nonNullPrint(<!TYPE_MISMATCH!>foo.value<!>)
        }
        if (<!DEBUG_INFO_SMARTCAST!>foo<!>.isError()) {
            foo.<!UNRESOLVED_REFERENCE!>error<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>printStackTrace<!>()
        }
    }
}

fun nonNullPrint(printMe: String) {
    println(printMe)
}
