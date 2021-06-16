interface Common
interface C1 : Common
interface C2 : Common

context(Common)
fun foo() {}

fun Common.bar() {}

context(C1, C2)
fun test() {
    foo()
    bar()
}