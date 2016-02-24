// FILE: A.kt

package aaa

class A {
    class B {
        class O {
          val s = "OK"
        }
    }
}

// FILE: B.kt

fun main(args: Array<String>) {
    val str = aaa.A.B.O().s
    if (str != "OK") {
        throw Exception()
    }
}
