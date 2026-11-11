// FILE: utils.kt

inline fun spreadTwice(vararg values: String) {
    java.util.Arrays.asList(*values)
    java.util.Arrays.asList(*values)
}

// FILE: test.kt

fun test() {
    spreadTwice("a", "b")
}

// @TestKt.class:
// 2 copyOf
