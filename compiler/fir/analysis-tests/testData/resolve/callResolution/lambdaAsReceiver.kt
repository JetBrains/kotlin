fun (() -> String).foo() {}
fun String.foo() {}

fun String.bar() {}

fun main1() {
    { "" }.foo()
    "".foo()
}

fun main2() {
    { "" }.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
    "".bar()
}
