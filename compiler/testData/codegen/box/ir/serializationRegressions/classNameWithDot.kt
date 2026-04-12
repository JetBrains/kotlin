// ISSUE: KT-85639
// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: lib.kt
class `C.c`(val v: String)

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val instance = `C.c`("OK")
    return instance.v
}
