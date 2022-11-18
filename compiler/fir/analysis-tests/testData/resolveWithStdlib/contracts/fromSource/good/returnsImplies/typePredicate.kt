import kotlin.contracts.*

fun checkIsString(x: Any): Boolean {
    contract {
        returns(true) implies (x is String)
        returns(false) implies (x !is String)
    }
    return x is String
}

fun test(x: Any) {
    if (checkIsString(x)) {
        x.length // OK
    } else {
        <!ARGUMENT_TYPE_MISMATCH!>x.<!UNRESOLVED_REFERENCE!>length<!><!> // Error
    }
}