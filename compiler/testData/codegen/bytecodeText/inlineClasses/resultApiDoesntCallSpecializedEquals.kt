// !API_VERSION: 1.3
// WITH_STDLIB
// FILE: test.kt
fun test() {
    val result = Result.success("yes!")
    val other = Result.success("nope")
    if (result == other) println("==")
    if (result != other) println("!=")
}

// @TestKt.class:
// 0 INVOKESTATIC kotlin/Result.equals-impl0
