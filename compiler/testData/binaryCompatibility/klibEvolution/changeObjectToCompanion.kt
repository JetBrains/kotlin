// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class N {
    fun bar() = "something in N"
}

class X {
    fun foo() = "without companion"

    object W : N() {
        val qux = "this is in object"
    }

}

// FILE: B.kt
// VERSION: 2

open class N {
    fun bar() = "something in N"
}

class X {
    fun foo() = "with companion"

    companion object W : N() {
        val qux = "this is in companion object"
    }
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String = when {
    X.W.qux != "this is in companion object" -> "fail 1"
    X.W.bar() != "something in N" -> "fail 2"
    X().foo() != "with companion" -> "fail 3"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

