package c

import b.a.A

fun bar() {
    val t: A = A()
    b.a.foo()
    println(b.a.x)
    b.a.x = ""
}
