// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

fun foo(block: (A) -> Unit) { }
fun foobar(block: (A, B) -> Unit) { }

fun bar() {
    foo { (a, <!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>) ->
        a checkType { _<Int>() }
    }

    foo { (<!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>a<!>, b) ->
        b checkType { _<String>() }
    }

    foo { (a: Int, <!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>: String) ->
        a checkType { _<Int>() }
    }

    foo { (<!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>a<!>: Int, b: String) ->
        b checkType { _<String>() }
    }

    foobar { (a, <!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>), c ->
        a checkType { _<Int>() }
    }

    foobar { a, (<!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>, c) ->
        c checkType { _<Short>() }
    }

    foobar { (a, <!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>b<!>), (<!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>c<!>, d) ->
        a checkType { _<Int>() }
        d checkType { _<Short>() }
    }

    foobar { (<!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>a<!>, b), (c, <!UNUSED_DESTRUCTURED_PARAMETER_ENTRY!>d<!>) ->
        b checkType { _<String>() }
        c checkType { _<Double>() }
    }
}
