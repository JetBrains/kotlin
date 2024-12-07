// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class N {
    fun bar() = "something in N"
}

class X {
    fun foo() = "with companion"

    companion object : N() {
        val qux = "this is in companion object"
    }
}

// FILE: B.kt
// VERSION: 2

open class N {
    fun bar() = "something in N"
}

class X {
    fun foo() = "without companion"

    object Companion : N() {
        val qux = "this is in object"
    }

}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String = when {
    X.qux != "this is in object" -> "fail 1"
    X.bar() != "something in N" -> "fail 2"
    X().foo() != "without companion" -> "fail 3"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

