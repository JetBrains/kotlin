// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

interface Foo {
    fun ok(x: Int, y: String = "")

    fun failX(x: Int, y: String = "")

    fun failY(x: Int, y: String = "")
}

expect class Bar : Foo {
    override fun ok(x: Int, y: String)

    override fun failX(x: Int, y: String)

    override fun failY(x: Int, y: String)
}

fun test(foo: Foo, bar: Bar) {
    foo.ok(42)
    foo.ok(42, "OK")
    bar.ok(42)
    bar.ok(42, "OK")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Bar : Foo {
    actual override fun ok(x: Int, y: String) {}

    actual override fun failX(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>x: Int = <!DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE!>0<!><!>, y: String) {}

    actual override fun failY(x: Int, <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>y: String = <!DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE!>""<!><!>) {}
}
