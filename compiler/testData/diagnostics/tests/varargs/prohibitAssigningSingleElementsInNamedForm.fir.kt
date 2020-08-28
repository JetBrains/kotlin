// !LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER

annotation class Anno1(vararg val s: String)
annotation class Anno2(vararg val i: Int)

@Anno1(s = "foo")
@Anno2(i = *intArrayOf(1))
fun f1() {}

@Anno1(s = ["foo"])
@Anno2(i = intArrayOf(1))
fun f2() {}

fun foo(vararg ints: Int) {}

fun test() {
    foo(ints = 1)
    foo(ints = *intArrayOf(1))
}
