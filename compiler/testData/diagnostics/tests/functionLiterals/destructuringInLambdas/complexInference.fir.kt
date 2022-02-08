// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

// first parameter of the functional type of 'x' can only be inferred from a lambda parameter explicit type specification
fun <X, Y> foo(y: Y, x: (X, Y) -> Unit) {}

fun bar(aInstance: A, bInstance: B) {
    foo("") {
        (a, b): A, c ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
        c checkType { _<String>() }
    }

    foo(aInstance) {
        a: String, (b, c) ->
        a checkType { _<String>() }
        b checkType { _<Int>() }
        c checkType { _<String>() }
    }

    foo(bInstance) {
        (a, b): A, (c, d) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(bInstance) {
        <!CANNOT_INFER_PARAMETER_TYPE, COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a, b)<!>, (c, d) ->
        a <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Int>() }
        b <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }

    foo<A, B>(bInstance) {
        (a, b), (c, d) ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }
}
