// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val x: Int, val y: String)

fun bar() {
    val x = { (a, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    x checkType { _<(A) -> Unit>() }

    val y = { (a: Int, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    y checkType { _<(A) -> Unit>() }

    val y2 = { (a: Number, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    y2 checkType { _<(A) -> Unit>() }

    val z = { <!CANNOT_INFER_PARAMETER_TYPE!>(<!NI;TYPE_MISMATCH!>a: Int<!>, <!NI;TYPE_MISMATCH!>b: String<!>)<!> ->
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    }
}
