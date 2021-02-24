// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun <T : Any> Array<T?>.filterNotNull(): List<T> = throw Exception()

fun test1(a: Array<out Int?>) {
    val list = a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>filterNotNull<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>list<!> <!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>checkType<!> { <!UNRESOLVED_REFERENCE{NI}!>_<!><List<Int>>() }
}

fun test2(vararg a: Int?) {
    val list = a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>filterNotNull<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>list<!> <!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>checkType<!> { <!UNRESOLVED_REFERENCE{NI}!>_<!><List<Int>>() }
}
