fun (() -> String).foo() {}
fun String.foo() {}

fun String.bar() {}

fun main1() {
    { "" }.foo()
    "".foo()
}

fun main2() {
    { "" }.<!INAPPLICABLE_CANDIDATE!>bar<!>()
    "".bar()
}
