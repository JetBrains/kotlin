// ISSUE: KT-57986

// MODULE: m1
// FILE: m1.kt

fun foo() = 42

val x = foo()
val y = x.toLong()

// MODULE: m2(m1)
// FILE: m2.kt

fun box(): String {
    if (y == 42L) {
        return "OK"
    }
    return y.toString()
}
