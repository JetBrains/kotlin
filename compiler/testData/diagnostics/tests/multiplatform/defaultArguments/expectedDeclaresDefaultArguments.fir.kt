// MODULE: m1-common
// FILE: common.kt

expect fun ok(x: Int, y: String = "")

expect fun failX(x: Int, y: String = "")

expect fun failY(x: Int, y: String = "")

expect open class Foo {
    fun ok(x: Int, y: String = "")

    fun failX(x: Int, y: String = "")

    fun failY(x: Int, y: String = "")
}

fun test(foo: Foo) {
    ok(42)
    ok(42, "OK")
    foo.ok(42)
    foo.ok(42, "OK")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun ok(x: Int, y: String) {}

actual fun failX(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>x: Int = 0<!>, y: String) {}

actual fun failY(x: Int, <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>y: String = ""<!>) {}

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_ERROR!>class Foo<!> {
    actual fun ok(x: Int, y: String) {}

    actual fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_ERROR!>failX<!>(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>x: Int = 0<!>, y: String) {}

    actual fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_ERROR!>failY<!>(x: Int, <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>y: String = ""<!>) {}
}
