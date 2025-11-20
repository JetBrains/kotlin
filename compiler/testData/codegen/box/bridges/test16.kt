// WITH_STDLIB

import kotlin.test.*

interface A {
    fun foo(): String
}

abstract class C: A

open class B: C() {
    override fun foo(): String {
        return "OK"
    }
}

fun bar(c: C) = c.foo()

fun box(): String {
    val b = B()
    val c: C = b
    val barb = bar(b)
    if (barb != "OK") return "FAIL b: $barb"

    val barc = bar(c)
    if (barc != "OK") return "FAIL c: $barc"

    return "OK"
}
