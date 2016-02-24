// FILE: A.kt

package aaa

class A {
    class O {
      val s = "OK"
    }
}

// FILE: B.kt

fun main(args: Array<String>) {
    val str = aaa.A.O().s
    if (str != "OK") {
        throw Exception()
    }
}
