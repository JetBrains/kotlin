// MODULE: lib
// FILE: A.kt
// VERSION: 1

enum class X {
    Y,
    Z
}

// FILE: B.kt
// VERSION: 2

enum class X {
    Y,
    Z,
    W
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String = when {
    X.values().map { it.name }.joinToString(", ") != "Y, Z, W" -> "fail 1"
    X.valueOf("W").name != "W" -> "fail 2"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

