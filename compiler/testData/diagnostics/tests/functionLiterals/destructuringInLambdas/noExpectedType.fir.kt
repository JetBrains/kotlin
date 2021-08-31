// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE
data class A(val x: Int, val y: String)

fun bar() {
    val x = { (a, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    x checkType { _<(A) -> Unit>() }

    val y = { (a: Int, b): A ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }

    y checkType { _<(A) -> Unit>() }

    val y2 = { (a: Number, b): A ->
        a checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Int>() }
        b checkType { _<String>() }
    }

    y2 checkType { _<(A) -> Unit>() }

    val z = { <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>(a: Int, b: String)<!> ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }
}
