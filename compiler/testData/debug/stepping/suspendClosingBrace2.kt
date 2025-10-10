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
        funWithSuspendLast()
    }
}

suspend fun funWithSuspendLast() {
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:8 box
// test.kt:12 box
// test.kt:17 funWithSuspendLast
// test.kt:12 box
// test.kt:6 box
// test.kt:14 box
