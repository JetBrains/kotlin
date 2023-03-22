// IGNORE_REVERSED_RESOLVE
annotation class Foo(val a: IntArray, val b: Array<String>, val c: FloatArray)

@Foo([1], ["/"], [1f])
fun test1() {}

@Foo([], [], [])
fun test2() {}

@Foo([1f], [' '], [1])
fun test3() {}

@Foo(c = [1f], b = [""], a = [1])
fun test4() {}

@Foo([1 + 2], ["Hello, " + "Kotlin"], [<!DIVISION_BY_ZERO!>1 / 0f<!>])
fun test5() {}

const val ONE = 1
val two = 2

@Foo([ONE], [], [])
fun test6() {}

@Foo(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>ONE + two<!>]<!>, [], [])
fun test7() {}

@Foo(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>two<!>]<!>, [], [])
fun test8() {}
