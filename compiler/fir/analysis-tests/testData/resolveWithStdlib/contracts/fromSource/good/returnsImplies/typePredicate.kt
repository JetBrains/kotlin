import kotlin.contracts.*

fun checkIsString(x: Any) {
    contract {
        returns(true) implies (x is String)
        returns(false) implies (x !is String)
    }
    return <!RETURN_TYPE_MISMATCH!>x is String<!>
}

fun test(x: Any) {
    if (checkIsString(x)) {
        x.length // OK
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!> // Error
    }
}