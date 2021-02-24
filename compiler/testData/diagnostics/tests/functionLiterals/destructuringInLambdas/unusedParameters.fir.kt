// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A) -> Unit) { }
fun foobar(block: (A, B) -> Unit) { }

fun bar() {
    foo { (a, b) ->
        a checkType { _<Int>() }
    }

    foo { (a, b) ->
        b checkType { _<String>() }
    }

    foo { (a: Int, b: String) ->
        a checkType { _<Int>() }
    }

    foo { (a: Int, b: String) ->
        b checkType { _<String>() }
    }

    foobar { (a, b), c ->
        a checkType { _<Int>() }
    }

    foobar { a, (b, c) ->
        c checkType { _<Short>() }
    }

    foobar { (a, b), (c, d) ->
        <!UNRESOLVED_REFERENCE!>a<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        d checkType { _<Short>() }
    }

    foobar { (a, b), (c, d) ->
        <!UNRESOLVED_REFERENCE!>b<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        c checkType { _<Double>() }
    }
}
