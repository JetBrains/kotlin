// FILE: test.kt

fun <T> eval(f: () -> T) = f()

fun box() {
    eval {
        "OK"
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test.kt:3 eval
// test.kt:7 invoke
// test.kt:3 eval
// test.kt:6 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:3 eval
// test.kt:7 box$lambda
// test.kt:9 box