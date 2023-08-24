// IGNORE_BACKEND: WASM
// WITH_STDLIB
// FILE: test.kt

val strings = arrayOf("1a", "1b", "2", "3")

fun box() {
    for (s in strings) {
        if (s == "1a" || s == "1b") {
            continue
        }
        if (s == "2") {
            continue
        }
        println(s)
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:15 box
// test.kt:8 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:5 <get-strings>
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:15 box
// test.kt:8 box
// test.kt:17 box
