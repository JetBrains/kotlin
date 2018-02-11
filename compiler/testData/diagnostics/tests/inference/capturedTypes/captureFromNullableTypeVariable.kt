// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun <T : Any> Array<T?>.filterNotNull(): List<T> = throw Exception()

fun test1(a: Array<out Int?>) {
    val list = a.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>filterNotNull<!>()
    <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>list<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!NI;UNRESOLVED_REFERENCE!>_<!><List<Int>>() }
}

fun test2(vararg a: Int?) {
    val list = a.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>filterNotNull<!>()
    <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>list<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!NI;UNRESOLVED_REFERENCE!>_<!><List<Int>>() }
}