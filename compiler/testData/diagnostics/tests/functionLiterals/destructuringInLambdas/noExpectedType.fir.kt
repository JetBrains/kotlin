// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val x: Int, val y: String)

fun bar() {
    val x = { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a, b): A<!> ->
        a <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    x checkType { _<(A) -> Unit>() }

    val y = { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a: Int, b): A<!> ->
        a checkType { _<Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    y checkType { _<(A) -> Unit>() }

    val y2 = { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a: Number, b): A<!> ->
        a checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
        b <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }

    y2 checkType { _<(A) -> Unit>() }

    val z = { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a: Int, b: String)<!> ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }
}
