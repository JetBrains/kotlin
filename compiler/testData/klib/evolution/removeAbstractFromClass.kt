// MODULE: lib
// FILE: A.kt
// VERSION: 1

abstract class X {
    fun foo(): String = "in abstract class"
    val bar: String = "in abstract class"
}

// FILE: B.kt
// VERSION: 2

open class X {
    fun foo(): String = "in non-abstract class"
    val bar: String = "in non-abstract class"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Y: X() 

fun lib(): String = when {
    Y().foo() != "in non-abstract class" -> "fail 1"
    Y().bar != "in non-abstract class" -> "fail 2"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

