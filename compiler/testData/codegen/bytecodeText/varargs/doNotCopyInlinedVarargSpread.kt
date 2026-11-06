// FILE: utils.kt

inline fun spreadOnly(vararg values: String) {
    java.util.Arrays.asList(*values)
}

inline fun spreadEmpty(vararg values: String) {
    java.util.Arrays.asList(*values)
}

inline fun spreadSingleValue(vararg values: Any?) {
    java.util.Arrays.asList(*values)
}

// FILE: test.kt

fun test(x: Any?) {
    spreadOnly("a", "b", "c")
    spreadEmpty()
    spreadSingleValue(x)
}

// @TestKt.class:
// 0 copyOf
