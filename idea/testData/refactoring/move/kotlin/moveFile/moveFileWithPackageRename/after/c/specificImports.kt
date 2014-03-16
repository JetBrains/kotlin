package c

import b.Test
import b.test
import b.TEST

fun bar() {
    val t: Test = Test()
    test()
    println(TEST)
    TEST = ""
}
