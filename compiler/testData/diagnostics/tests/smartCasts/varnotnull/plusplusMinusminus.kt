// !WITH_NEW_INFERENCE
fun foo(arg: Int?): Int {
    var i = arg
    if (i != null && <!DEBUG_INFO_SMARTCAST!>i<!>++ == 5) {
        return <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>i<!>--<!> + <!DEBUG_INFO_SMARTCAST!>i<!>
    }
    return 0
}

operator fun Long?.inc() = this?.let { it + 1 }

fun bar(arg: Long?): Long {
    var i = arg
    if (i++ == 5L) {
        return <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>i<!UNSAFE_CALL!>--<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> i<!>
    }
    if (i++ == 7L) {
        return i++ <!NI;NONE_APPLICABLE, OI;UNSAFE_OPERATOR_CALL!>+<!> <!OI;TYPE_MISMATCH!>i<!>
    }
    return 0L
}