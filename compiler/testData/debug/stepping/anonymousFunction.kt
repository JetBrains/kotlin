// FILE: test.kt

fun <T> eval(f: () -> T) = f()

fun box() {
    eval {
        "OK"
    }
}

// LINENUMBERS
// test.kt:6 box
// test.kt:3 eval
// test.kt:7 invoke
// test.kt:3 eval
// test.kt:6 box
// test.kt:9 box