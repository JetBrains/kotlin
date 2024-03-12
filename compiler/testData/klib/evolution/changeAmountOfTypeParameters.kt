// KT-44598: Expected <OK>, actual <fail 1: XZ>
// IGNORE_BACKEND: JS_IR, NATIVE

// MODULE: lib
// FILE: A.kt
// VERSION: 1
class A<X, Z, W>() {
    fun types() = "XZW"
}

// FILE: B.kt
// VERSION: 2
class A<X, Z>() {
    fun types() = "XZ"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String {
    val types = A<String, String, Int>().types()
    return when {
        types != "XZW" -> "fail 1: $types"
        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()
