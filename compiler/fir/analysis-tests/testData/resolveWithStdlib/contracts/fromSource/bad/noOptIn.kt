import kotlin.contracts.*

fun checkIsString(x: Any): Boolean {
    <!OPT_IN_USAGE_ERROR!>contract<!> {
        <!OPT_IN_USAGE_ERROR!>returns<!>(true) <!OPT_IN_USAGE_ERROR!>implies<!> (x is String)
        <!OPT_IN_USAGE_ERROR!>returns<!>(false) <!OPT_IN_USAGE_ERROR!>implies<!> (x !is String)
    }
    return x is String
}

fun test(x: Any) {
    if (checkIsString(x)) {
        x.length // OK
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!> // Error
    }
}
