// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class X {
    private fun foo() = "private in super"
    fun bar() = foo()

    private val zeg = "private in super"
    fun lim() = zeg
}

// FILE: B.kt
// VERSION: 2

open class X {
    public fun foo() = "public in super"
    fun bar() = foo()

    private val zeg = "public in super"
    fun lim() = zeg
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

class Y: X() {
    private fun foo() = "private in derived"
    fun qux() = foo()

    private val zeg = "private in derived"
    fun tes() = zeg
}

fun lib(): String = when {
    X().bar() != "public in super" -> "fail 1"
    Y().bar() != "public in super" -> "fail 2"
    Y().qux() != "private in derived" -> "fail 3"

    X().lim() != "public in super" -> "fail 4"
    Y().lim() != "public in super" -> "fail 5"
    Y().tes() != "private in derived" -> "fail 6"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

