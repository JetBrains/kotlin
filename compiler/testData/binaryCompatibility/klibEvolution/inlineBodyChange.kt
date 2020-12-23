// MODULE: lib
// FILE: A.kt
// VERSION: 1

inline fun foo() = "foo before change"

class X {
    inline fun bar() = "bar before change"
}

// FILE: A.kt
// VERSION: 2

inline fun foo() = "foo after change"

class X {
    inline fun bar() = "bar after change"
}


// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String = when {
    foo() != "foo after change" -> "fail 1"
    X().bar() != "bar after change" -> "fail 2"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

