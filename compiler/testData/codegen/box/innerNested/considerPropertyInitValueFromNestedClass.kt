// ISSUE: KT-52057
// MODULE: lib
// FILE: Class.kt

annotation class Ann(val p: String)

class Class {
    object Obj {
        const val Const = "const"
    }
}

// MODULE: main(lib)
// FILE: main.kt

import Class

@Ann("${Class.Obj.Const}+")
fun f(): String = "OK"

fun box() = f()