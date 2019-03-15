// IGNORE_BACKEND: JVM_IR
// FILE: test.kt

fun test1() {
    val n = null
    val u1 = n.elvis { "X1" }
    val u2 = "X2".elvis { "X3" }
}

// @TestKt.class:
// 0 IFNULL
// 0 IFNONNULL
// 1 X1
// 1 X2
// 0 X3

// FILE: inline.kt
inline fun <T : Any> T?.elvis(rhs: () -> T): T = this ?: rhs()
