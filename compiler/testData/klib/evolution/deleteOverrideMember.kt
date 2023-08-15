// MODULE: base
// FILE: base.kt

open class X {
    open fun foo() = "base function"
    open val bar = "base property"
}

// MODULE: lib(base)
// FILE: A.kt
// VERSION: 1

open class Y: X() {
    override fun foo() = "overridden function"
    override val bar = "overridden property"
}

// FILE: B.kt
// VERSION: 2

open class Y: X() 

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Z: Y() {
    override fun foo() = "primordial overridden function"
    override val bar = "primordial overridden property"
}

fun lib(): String {
    val y = Y()
    val z = Z()
    return when {
        y.foo() != "base function" -> "fail 1"
        y.bar != "base property" -> "fail 2"

        z.foo() != "primordial overridden function" -> "fail 5"
        z.bar != "primordial overridden property" -> "fail 6"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

