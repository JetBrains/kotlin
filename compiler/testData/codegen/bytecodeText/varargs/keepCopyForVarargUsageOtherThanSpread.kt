// FILE: utils.kt

inline fun spreadAndCount(vararg values: String): Int {
    java.util.Arrays.asList(*values)
    return values.size
}

// FILE: test.kt

fun test(): Int = spreadAndCount("a", "b")

// @TestKt.class:
// 1 copyOf
