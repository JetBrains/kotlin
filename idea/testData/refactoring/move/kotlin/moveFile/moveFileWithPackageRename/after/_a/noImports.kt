package a

import b.Test

fun bar() {
    val t: Test = Test()
    b.test()
    println(b.TEST)
    b.TEST = ""
}
