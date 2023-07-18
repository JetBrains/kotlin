annotation class Foo(val a: IntArray, val b: Array<String>, val c: FloatArray)

@Foo([1], ["/"], [1f])
fun test1() {}

@Foo([], [], [])
fun test2() {}

@Foo([<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1f<!>], <!TYPE_MISMATCH!>[' ']<!>, [<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>])
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

interface I<T>
class C<T> : I<T>

annotation class Test1<T>(val x: Int)
annotation class Test2<T1, T2 : I<T1>>(val x: Test1<I<T2>>)
@Repeatable annotation class Test3(val x: Array<Test2<Int, C<Int>>>)

@Test3(<!TYPE_MISMATCH!>[Test2<String, C<String>>(Test1(40))]<!>)
@Test3([Test2<Int, C<Int>>(Test1(40))])
@Test3([Test2(Test1(40))])
fun test9() {}
