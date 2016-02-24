// FILE: A.kt

package aaa

class A {
    enum class E {
      A
    }
}

// FILE: B.kt

fun main(args: Array<String>) {
    val str = aaa.A.E.A
    if (str.toString() != "A") {
        throw Exception()
    }
}
