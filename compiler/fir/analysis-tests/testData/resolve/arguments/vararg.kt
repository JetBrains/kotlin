fun foo(x: Int, vararg y: String) {}
fun bar(x: Int, vararg y: String, z: Boolean) {}

fun test() {
    foo(1)
    foo(1, "")
    foo(1, "my", "yours")
    foo(1, *arrayOf("my", "yours"))

    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    foo(1, <!ARGUMENT_TYPE_MISMATCH!>2<!>)

    bar(1, z = true, y = *arrayOf("my", "yours"))

    bar(0, z = false, y = "", <!ARGUMENT_PASSED_TWICE!>y<!> = "other")
    bar(0, "", true<!NO_VALUE_FOR_PARAMETER!>)<!>
    bar(0, z = false, y = "", <!ARGUMENT_PASSED_TWICE!>y<!> = "other", <!ARGUMENT_PASSED_TWICE!>y<!> = "yet other")
}
