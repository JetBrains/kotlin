import kotlin.contracts.*

fun checkNotNull(x: Any?) {
    contract {
        returns(true) implies (x != null)
        returns(false) implies (x == null)
    }
    return <!RETURN_TYPE_MISMATCH!>x != null<!>
}

fun trickyRequireNotNull(x: Any?) {
    contract {
        returns() implies (!(x == null))
    }
    if (x == null) {
        throw IllegalStateException()
    }
}

fun test_1(x: String?) {
    if (checkNotNull(x)) {
        x.length // OK
    } else {
        x<!UNSAFE_CALL!>.<!>length // Error
    }
}

fun test_2(x: String?) {
    trickyRequireNotNull(x)
    x.length
}