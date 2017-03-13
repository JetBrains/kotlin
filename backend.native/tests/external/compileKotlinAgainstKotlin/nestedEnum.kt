// FILE: A.kt

package aaa

class A {
    enum class E {
        A
    }
}

// FILE: B.kt

fun box(): String {
    val str = aaa.A.E.A
    if (str.toString() != "A") {
        return "Fail $str"
    }
    return "OK"
}
