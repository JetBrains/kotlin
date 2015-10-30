package anotherPackage

import First

class Second : First() {
    val some = { First.TEST }
    fun foo() = { First.test() }

    val some2 = { TEST }
    fun foo2() = { test() }
}

fun box(): String {
    if (Second().some.invoke() != "OK") return "fail 1"

    if (Second().foo().invoke() != "OK") return "fail 2"

    if (Second().some2.invoke() != "OK") return "fail 3"

    if (Second().foo2().invoke() != "OK") return "fail 4"

    return "OK"
}