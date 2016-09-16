// !LANGUAGE: -DestructuringLambdaParameters
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A(val x: Int, val y: String)

fun foo(block: (A) -> Unit) { }

fun bar() {
    foo { <!UNSUPPORTED_FEATURE!>(a, b)<!> ->
        a checkType { _<Int>() }
        b checkType { _<String>() }
    }
}
