// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)
data class B(val u: Double, val w: Short)

// first parameter of the functional type of 'x' can only be inferred from a lambda parameter explicit type specification
fun <X, Y> foo(y: Y, x: (X, Y) -> Unit) {}

fun bar(aInstance: A, bInstance: B) {
    foo("") {
        <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a, b): A<!>, c ->
        a <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
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
        <!UNRESOLVED_REFERENCE!>a<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        <!UNRESOLVED_REFERENCE!>b<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }

    foo(bInstance) {
        (a, b), (c, d) ->
        <!UNRESOLVED_REFERENCE!>a<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        <!UNRESOLVED_REFERENCE!>b<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }

    foo<A, B>(bInstance) {
        (a, b), (c, d) ->
        <!UNRESOLVED_REFERENCE!>a<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        <!UNRESOLVED_REFERENCE!>b<!> <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        c checkType { _<Double>() }
        d checkType { _<Short>() }
    }
}
