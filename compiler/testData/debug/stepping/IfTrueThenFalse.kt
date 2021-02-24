// FILE: test.kt
fun cond() = false

fun box() {
    if (cond())
        cond()
    else
         false
}

// LINENUMBERS
// test.kt:5 box
// test.kt:2 cond
// test.kt:5 box
// test.kt:8 box
// test.kt:9 box
