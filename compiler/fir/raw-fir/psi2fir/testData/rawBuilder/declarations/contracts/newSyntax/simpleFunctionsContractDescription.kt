// new contracts syntax for simple functions
fun test1(s: MyClass?) contract [returns() implies (s != null), returns() implies (s is MySubClass)] {
    test_1()
}

fun test2() contract [returnsNotNull()] {
    test2()
}