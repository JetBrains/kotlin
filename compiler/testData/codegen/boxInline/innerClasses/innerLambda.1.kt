import test.*

fun box(): String {
    val x1 = foo1()()
    if (x1 != "OK") return "fail 1: $x1"

    foo2()().run()

    return test.sideEffects
}
