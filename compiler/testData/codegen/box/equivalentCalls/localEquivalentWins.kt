// MODULE: lib
// FILE: lib.kt
val a = "FAIL 1"

// MODULE: main(lib)
// FILE: box.kt
private val a = "OK"

fun box(): String {
    if (a != "OK") return a

    return "OK"
}