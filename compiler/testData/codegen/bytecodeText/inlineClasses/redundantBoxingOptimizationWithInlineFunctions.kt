// FILE: test.kt

@JvmInline
value class I(val v: Int)

fun f1(a: I) { }
fun <T> f2(a: T) {}
inline fun <T> f3(a: T) { }

fun main() {
    f1(I(1)) // no boxing
    f2(I(1)) // boxing
    f3(I(1)) // no boxing
}

// @TestKt.class:
// 3 INVOKESTATIC I.constructor-impl \(I\)I
// 1 INVOKESTATIC I.box-impl \(I\)LI;
