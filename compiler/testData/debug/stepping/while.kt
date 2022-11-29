// FILE: test.kt

fun box() {
    var x = 2
    while (--x > 0) {
        "OK"
    }

    x = 2
    do {
        "OK"
    } while (--x > 0)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:5 box
// test.kt:9 box
// test.kt:11 box
// test.kt:12 box
// test.kt:11 box
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:5 box
// test.kt:5 box
// test.kt:5 box
// test.kt:9 box
// test.kt:12 box
// test.kt:12 box
// test.kt:12 box
// test.kt:12 box
// test.kt:13 box