// WITH_RUNTIME

// FILE: Ints.kt
val ints = listOf(1, 2, 3)

// FILE: Test.kt
fun test1(i: Int) =
        if (i in ints) "Yes" else "No"

fun test2(i: Int) =
        if (i !in ints) "Yes" else "No"

// @TestKt.class:
// 0 ICONST_0
// 0 ICONST_1
// 0 IXOR
