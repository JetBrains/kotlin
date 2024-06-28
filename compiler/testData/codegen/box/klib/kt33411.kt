// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_x64
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE && target=linux_x64
// IGNORE_BACKEND_K1: JVM, JVM_IR
// IGNORE_LIGHT_ANALYSIS

// ISSUE: KT-33411, KT-66338

// MODULE: m1
// FILE: m1.kt
fun f() {}
fun getO() = "O"

// MODULE: m2
// FILE: m2.kt
fun f() {}
fun getK() = "K"

// MODULE: main(m1)(m2)
// FILE: main.kt

fun box(): String {
    f()
    return getO() + getK()
}
