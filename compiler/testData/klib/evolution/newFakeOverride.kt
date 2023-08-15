// MODULE: base
// FILE: A.kt
// VERSION: 1

open class X 

// FILE: B.kt
// VERSION: 2

open class X {
    open fun foo(): String = "new member"
}

// MODULE: child(base)
// FILE: C.kt

class Y: X()

// MODULE: lib(child)
// FILE: D.kt
// VERSION: 1

fun qux(): String = "no foo() exists in ${Y()}"

// FILE: E.kt
// VERSION: 2

fun qux(): String = Y().foo()

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String = when {
    qux() != "new member" -> "fail 1"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

