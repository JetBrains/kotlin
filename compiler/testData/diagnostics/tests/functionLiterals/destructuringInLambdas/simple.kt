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
        a checkType { _<Int>() }
        b checkType { _<String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }

    foo { (<!NI;TYPE_MISMATCH, OI;COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>a: String<!>, b) ->
        <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!NI;UNRESOLVED_REFERENCE!>_<!><Int>() }
        b checkType { _<String>() }
    }

    foo <!NI;TYPE_MISMATCH!>{ <!OI;EXPECTED_PARAMETER_TYPE_MISMATCH!>(a, b): B<!> ->
        a checkType { _<Double>() }
        b checkType { _<Short>() }
    }<!>
}
