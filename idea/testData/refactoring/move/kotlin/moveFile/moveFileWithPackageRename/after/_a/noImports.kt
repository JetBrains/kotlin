package a

import b.TEST
import b.Test
import b.test

fun bar() {
    val t: Test = Test()
    test()
    t.test()
    println(TEST)
    println(t.TEST)
    TEST = ""
    t.TEST = ""
}
