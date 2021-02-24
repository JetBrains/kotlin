// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A, B) -> Unit) { }

fun bar() {
    foo { (a, a), b ->
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    foo { (a, b), a ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    foo { a, (a, b) ->
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    foo { (a, b), (c, b) ->
        <!UNRESOLVED_REFERENCE!>a<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        c checkType { <!INAPPLICABLE_CANDIDATE!>_<!><B>() }
    }
}
