// FIR_IDENTICAL
// MODULE: lib
// FILE: lib.kt

val lock = "1"

// MODULE: main(lib)
// FILE: main.kt

private val lock = "2"

fun test() {
    lock
}
