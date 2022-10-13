// FILE: test.kt
fun cond() = false

fun box() {
    if (cond())
        cond()
    else
         false
}

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:2 cond
// test.kt:5 box
// test.kt:8 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:2 cond
// test.kt:9 box