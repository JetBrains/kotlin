// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class X {
    private fun foo() = "private fun in superclass"
    private val bar = "private val in superclass"
    private class Z {
        fun qux() = "fun in a provate inner class in superclass"
    }
    
    fun bar() = "${foo()} $bar ${Z()}"
}

// FILE: B.kt
// VERSION: 2

open class X {
    fun bar() = "no private references after change"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Y: X() 

fun lib(): String = when {
    X().bar() != "no private references after change" -> "fail 1"
    Y().bar() != "no private references after change" -> "fail 2"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

