// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun foo(x: Int): Int<!>

<!CONFLICTING_OVERLOADS!>fun callFromCommonCode(x: Int)<!> = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(x)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun foo(x: Int): Int {
    return x + 1
}

fun callFromJVM(x: Int) = foo(x)
