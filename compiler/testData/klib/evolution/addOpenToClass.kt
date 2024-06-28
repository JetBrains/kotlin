// MODULE: lib
// FILE: A.kt
// VERSION: 1

class X {
    fun foo(): String = "in final class"
    val bar: String = "in final class"
}

fun qux(): X = X()

// FILE: B.kt
// VERSION: 2

open class X {
    fun foo(): String = "in open class"
    val bar: String = "in open class"
}

class Y: X()

fun qux(): X = Y()

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String = when {
    X().foo() != "in open class" -> "fail 1"
    X().bar != "in open class" -> "fail 2"
    qux().foo() != "in open class" -> "fail 3"
    qux().bar != "in open class" -> "fail 4"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

