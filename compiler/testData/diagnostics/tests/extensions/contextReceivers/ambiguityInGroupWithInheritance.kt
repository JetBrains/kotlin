interface Common
interface C1 : Common
interface C2 : Common

context(Common)
fun foo() {}

fun Common.bar() {}

context(C1, C2)
fun test() {
    <!MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER!>foo()<!>
    bar()
}