// KT-44604: expected:<[OK]> but was:<[19]>
// IGNORE_BACKEND: JS_IR
// KT-44604: compilation failed: Failed IR validation BEFORE Compiler Phase @LowerBeforeInline
// IGNORE_BACKEND: NATIVE

// MODULE: lib
// FILE: A.kt
// VERSION: 1

fun qux(): String  = "OK"

// FILE: B.kt
// VERSION: 2

fun qux(): Int = 19

// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String {
    return qux()
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()
