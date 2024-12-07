// MODULE: lib
// FILE: A.kt
// VERSION: 1

fun foo() = "global"

class X {
    fun foo() = "member"
}

// FILE: A.kt
// VERSION: 2

inline fun foo() = "inline global"

class X {
    inline fun foo() = "inline member"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String = when {
    foo() != "inline global" -> "fail 1"
    X().foo() != "inline member" -> "fail 2"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

