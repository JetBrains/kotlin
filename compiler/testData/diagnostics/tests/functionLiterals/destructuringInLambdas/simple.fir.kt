// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A) -> Unit) { }
fun foobar(block: (A, B) -> Unit) { }

fun bar() {
    foo { (a, b) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    foo { (a: Int, b: String) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    foo { (a, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    foobar { (a, b), c ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
        c checkType { _<B>() }
    }

    foobar { a, (b, c) ->
        a checkType { _<A>() }
        b checkType { _<Double>() }
        c checkType { _<Short>() }
    }

    foobar { (a, b), (c, d) ->
        <!UNRESOLVED_REFERENCE!>a<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        <!UNRESOLVED_REFERENCE!>b<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }

    foo { (a: String, b) ->
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b checkType { _<String>() }
    }

    <!INAPPLICABLE_CANDIDATE!>foo<!> { (a, b): B ->
        a checkType { _<Double>() }
        b checkType { _<Short>() }
    }
}
