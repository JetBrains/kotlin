// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt

val sb = StringBuilder()

inline fun foo() {
    do {
        var x: Int = 999
        sb.append("OK")
    } while (x != 999)
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    foo()
    return sb.toString()
}