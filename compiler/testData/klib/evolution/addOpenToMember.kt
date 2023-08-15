// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class X {
    fun foo(): String = "final method"
    val bar: String = "final property"
}

class Y: X()

// FILE: B.kt
// VERSION: 2

open class X {
    open fun foo(): String = "open method"
    open val bar: String = "open property"
}

class Y: X() {
    override fun foo(): String = "derived method"
    override val bar: String = "derived property"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Z: X() 

fun lib(): String = when {
    X().foo() != "open method" -> "fail 1"
    X().bar != "open property" -> "fail 2"
    Y().foo() != "derived method" -> "fail 3"
    Y().bar != "derived property" -> "fail 4"
    Z().foo() != "open method" -> "fail 5"
    Z().bar != "open property" -> "fail 6"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

