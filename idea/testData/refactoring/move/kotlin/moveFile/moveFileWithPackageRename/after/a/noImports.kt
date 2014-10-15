package a

import b.test
import b.TEST
import b.Test

fun bar() {
    val t: Test = Test()
    test()
    t.test()
    println(TEST)
    println(t.TEST)
    TEST = ""
    t.TEST = ""
}
