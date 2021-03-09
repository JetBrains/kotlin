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
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:7 box
// test.kt:8 box
// test.kt:11 box
// test.kt:12 box
// test.kt:7 box
// test.kt:8 box
// test.kt:11 box
// test.kt:14 box
// test.kt:7 box
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:4 <get-strings>
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:8 box
// test.kt:11 box
// test.kt:12 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:8 box
// test.kt:11 box
// test.kt:14 box
// test.kt:7 box
// test.kt:16 box
