//FILE: test.kt
fun box() {
    val k = if (getA()
        && getB()
        && getC()
        && getD()) {
        true
    } else {
        false
    }
}

fun getA() = true

fun getB() = true

fun getC() = false

fun getD() = true

// LINENUMBERS
// test.kt:3
// test.kt:13
// test.kt:3
// test.kt:4
// test.kt:15
// test.kt:4
// test.kt:5
// test.kt:17
// test.kt:5
// test.kt:9
// test.kt:3
// test.kt:11
