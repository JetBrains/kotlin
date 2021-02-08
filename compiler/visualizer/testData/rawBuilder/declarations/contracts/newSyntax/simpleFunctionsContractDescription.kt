// FIR_IGNORE
// new contracts syntax for simple functions
//           [ERROR : MyClass]?                          [ERROR: unknown type]          [ERROR: unknown type]
//           │                                           │                              │
fun test1(s: MyClass?) contract [returns() implies (s != null), returns() implies (s is MySubClass)] {
//  [ERROR: not resolved]
//  │
    test_1()
}

fun test2() contract [returnsNotNull()] {
//  fun test2(): Unit
//  │
    test2()
}
