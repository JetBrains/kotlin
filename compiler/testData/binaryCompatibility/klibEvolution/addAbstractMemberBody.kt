// MODULE: lib
// FILE: A.kt
// VERSION: 1

abstract class X {
    abstract fun foo(): String
    abstract val bar: String
    
    fun qux() = "nothing"
}

// FILE: B.kt
// VERSION: 2

abstract class X {
    open fun foo(): String = "now with a body"
    open val bar: String get() = "now with a body"
    fun qux() = foo() + bar
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Y: X() {
    override fun foo() = "derived body"
    override val bar get() = "derived body"
}

fun lib(): String {
    val y = Y()
    return when {
        y.foo() != "derived body" -> "fail 1"
        y.bar != "derived body" -> "fail 2"
        y.qux() != "derived bodyderived body" -> "fail 3"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

