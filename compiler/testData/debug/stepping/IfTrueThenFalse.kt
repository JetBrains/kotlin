//FILE: test.kt
fun cond() = false

fun box() {
    if (cond())
        cond()
    else
         false
}

// LINENUMBERS
// test.kt:5
// test.kt:2
// test.kt:5
// test.kt:8
// test.kt:9
