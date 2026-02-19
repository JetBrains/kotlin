// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: test.kt

suspend fun box() {
    for (i in 1..1) {
        //Breakpoint!
        if (i == 2) {
            "".toString()
            println("i = $i")
        }
        funWithSuspendLast(
            1 + 1,
            "O" + "K"
        )
    }
}

suspend fun funWithSuspendLast(i: Int, s: String) {
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:8 box
// test.kt:13 box
// test.kt:14 box
// test.kt:12 box
// test.kt:20 funWithSuspendLast
// test.kt:12 box
// test.kt:6 box
// test.kt:17 box
