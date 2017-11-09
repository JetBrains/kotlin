// !CHECK_TYPE

fun foo(<!UNUSED_PARAMETER!>block<!>: (Int, String) -> Unit) { }
fun foobar(<!UNUSED_PARAMETER!>block<!>: (Double) -> Unit) { }

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

    foo { <!REDECLARATION, REDECLARATION, UNUSED_ANONYMOUS_PARAMETER!>`_`<!>, <!REDECLARATION, REDECLARATION!>`_`<!> ->
        _ checkType { _<String>() }
    }

    foo(fun(<!UNUSED_ANONYMOUS_PARAMETER!>x<!>: Int, _: String) {})
}
