// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun ok(x: Int, y: String = "")<!>

<!CONFLICTING_OVERLOADS!>expect fun failX(x: Int, y: String = "")<!>

<!CONFLICTING_OVERLOADS!>expect fun failY(x: Int, y: String = "")<!>

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun ok(x: Int, y: String = "")

    fun failX(x: Int, y: String = "")

    fun failY(x: Int, y: String = "")
}

<!CONFLICTING_OVERLOADS!>fun test(foo: Foo)<!> {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ok<!>(42)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ok<!>(42, "OK")
    foo.ok(42)
    foo.ok(42, "OK")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun ok(x: Int, y: String) {}

actual fun failX(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>x: Int = 0<!>, y: String) {}

actual fun failY(x: Int, <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>y: String = ""<!>) {}

actual open class Foo {
    actual fun ok(x: Int, y: String) {}

    actual fun failX(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>x: Int = 0<!>, y: String) {}

    actual fun failY(x: Int, <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>y: String = ""<!>) {}
}
