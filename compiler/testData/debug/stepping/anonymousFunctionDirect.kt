// FILE: test.kt

fun box() {
    {
        "OK"
    }()
}

// LINENUMBERS
// test.kt:4 box
// LINENUMBERS JVM
// test.kt:5 invoke
// LINENUMBERS JVM_IR
// test.kt:5 box$lambda-0
// LINENUMBERS
// test.kt:4 box
// test.kt:7 box
