// MODULE: base
// FILE: base1.kt
// VERSION: 1

open class X {
}

// FILE: base2.kt
// VERSION: 2

open class X {
    fun foo() = "non-open member function moved higher"
    val bar = "non-open member property moved higher"
    fun nux() = "non-open member function moved higher to cause conflict"
    val zip = "non-open member property moved higher to cause conflict"
    open fun ril() = "open member function moved higher"
    open val det = "open member property moved higher"
}


// MODULE: lib(base)
// FILE: A.kt
// VERSION: 1

open class Y: X() {
    fun foo() = "non-open member function"
    val bar = "non-open member property"
    fun nux() = "non-open member function"
    val zip = "non-open member property"
    open fun ril() = "open member function"
    open val det = "open member property"
}

// FILE: B.kt
// VERSION: 2

open class Y: X() {
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

// Note that frontend wouldn't allow us to have Any->String->Int chain of overrides.

class Z: X() {
    fun nux() = "non-open member function sudden conflict"
    val zip = "non-open member property sudden conflict"
}

class W: Y() {
    override fun ril() = "overridden open member function"
    override val det = "overridden open member property"
}

fun lib(): String {
    val y = Y()
    val z = Z()
    val w = W()
    return when {
        y.foo() != "non-open member function moved higher" -> "fail 1"
        y.bar != "non-open member property moved higher" -> "fail 2"
        y.nux() != "non-open member function moved higher to cause conflict" -> "fail 3"
        y.zip != "non-open member property moved higher to cause conflict" -> "fail 4"
        y.ril() != "open member function moved higher" -> "fail 5"
        y.det != "open member property moved higher" -> "fail 6"

        z.nux() != "non-open member function sudden conflict" -> "fail 7"
        z.zip != "non-open member property sudden conflict" -> "fail 8"

        w.ril() != "overridden open member function"-> "fail 9"
        w.det != "overridden open member property" -> "fail 10"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

