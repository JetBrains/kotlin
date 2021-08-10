// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A, B) -> Unit) { }

fun bar() {
    foo { (a, a), b ->
        a checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Int>() }
        b checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
    }

    foo { (a, b), a ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    foo { a, (a, b) ->
        a checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Int>() }
        b checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
    }

    foo { (a, b), (c, b) ->
        a checkType { _<Int>() }
        b checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
        c checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><B>() }
    }
}
