// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
}

fun f(block: () -> Unit) {
    block()
}

// LINENUMBERS
// test.kt:3
// test.kt:4
// test.kt:10
// test.kt:5
// test.kt:6
// test.kt:-1
// test.kt:-1
// test.kt:10
// test.kt:11
// test.kt:7
