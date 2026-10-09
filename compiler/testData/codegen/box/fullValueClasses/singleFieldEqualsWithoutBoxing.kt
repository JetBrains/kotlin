// ISSUE: KT-89978
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// LANGUAGE: +FullValueClasses
// The test checks an optimization which is implemented only for JS_IR backend

value class Single(val x: Int)
value class Outer(val single: Single)

// CHECK_NOT_CALLED_IN_SCOPE: scope=test function=equals
// CHECK_NEW_COUNT: function=test count=0
fun test() {
    val a1 = Single(1)
    val b1 = Single(1)
    val a2 = Single(2)

    assertTrue(a1 == b1)
    assertFalse(a1 == a2)
    assertTrue(Outer(a1) == Outer(b1))
    assertFalse(Outer(a1) == Outer(a2))
}

fun box(): String {
    test()
    return "OK"
}
