// MODULE: lib
// FILE: A.kt
// VERSION: 1

class X {
    fun foo() = "without companion"
}

// FILE: B.kt
// VERSION: 2

class X {
    fun foo() = "with companion"

    companion object {
        val bar = "this is a companion"
    }
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String = when {
    X().foo() != "with companion" -> "fail 1"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

