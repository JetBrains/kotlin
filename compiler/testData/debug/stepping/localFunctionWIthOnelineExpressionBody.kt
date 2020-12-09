// FILE: test.kt

fun box() {
    "OK"
    fun bar() = "OK"
    "OK"
    bar()
    "OK"
}

// LINENUMBERS
// test.kt:4 box
// LINENUMBERS JVM
// test.kt:5 box
// LINENUMBERS
// test.kt:6 box
// test.kt:7 box
// LINENUMBERS JVM
// test.kt:5 invoke
// LINENUMBERS JVM_IR
// test.kt:5 box$bar
// LINENUMBERS
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box