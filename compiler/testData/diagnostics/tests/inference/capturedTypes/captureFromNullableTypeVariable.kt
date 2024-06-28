// CHECK_TYPE

fun <T : Any> Array<T?>.filterNotNull(): List<T> = throw Exception()

fun test1(a: Array<out Int?>) {
    val list = a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>filterNotNull<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>list<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><List<Int>>() }
}

fun test2(vararg a: Int?) {
    val list = a.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>filterNotNull<!>()
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>list<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><List<Int>>() }
}
