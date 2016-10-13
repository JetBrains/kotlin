// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(block: (Int, String) -> Unit) { }
fun foobar(block: (Double) -> Unit) { }

fun bar() {
    foo { _, b ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hashCode<!>()
        b checkType { _<String>() }
    }

    foo { a, _ ->
        a checkType { _<Int>() }
        <!UNRESOLVED_REFERENCE!>_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hashCode<!>()
    }

    foo { _, _ ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hashCode<!>()
    }

    foo { _: Int, b: String ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hashCode<!>()
        b checkType { _<String>() }
    }

    foo { a: Int, _: String ->
        a checkType { _<Int>() }
        <!UNRESOLVED_REFERENCE!>_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hashCode<!>()
    }

    foo { _: Int, _: String ->
        <!UNRESOLVED_REFERENCE!>_<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>hashCode<!>()
    }

    foo { `_`, _ ->
        _ checkType { _<Int>() }
    }

    foo { _, `_` ->
        _ checkType { _<String>() }
    }

    foo { <!REDECLARATION, REDECLARATION!>`_`<!>, <!REDECLARATION, REDECLARATION!>`_`<!> ->
        _ checkType { _<String>() }
    }

    foo(fun(x: Int, _: String) {})
}
