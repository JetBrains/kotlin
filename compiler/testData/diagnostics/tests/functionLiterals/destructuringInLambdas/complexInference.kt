// !WITH_NEW_INFERENCE
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

    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(bInstance) {
        <!OI;CANNOT_INFER_PARAMETER_TYPE!>(<!NI;COMPONENT_FUNCTION_MISSING!>a<!>, <!NI;COMPONENT_FUNCTION_MISSING!>b<!>)<!>, (c, d) ->
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
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
