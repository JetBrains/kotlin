
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

// EXPECTATIONS JVM_IR
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
// test.kt:5 <init properties test.kt>
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

// EXPECTATIONS WASM
// test.kt:5 $<init properties test.kt> (22, 28, 34, 39, 22, 14)
// test.kt:8 $box (14)
// test.kt:5 $<get-strings> (0)
// test.kt:8 $box (4, 14, 4, 14, 9, 14)
// test.kt:9 $box (12, 17, 12)
// test.kt:10 $box (12)
// test.kt:8 $box (4, 14, 4, 14, 9, 14)
// test.kt:9 $box (12, 17, 12, 25, 30, 25)
// test.kt:10 $box (12)
// test.kt:8 $box (4, 14, 4, 14, 9, 14)
// test.kt:9 $box (12, 17, 12, 25, 30, 25)
// test.kt:12 $box (12, 17, 12)
// test.kt:13 $box (12)
// test.kt:8 $box (4, 14, 4, 14, 9, 14)
// test.kt:9 $box (12, 17, 12, 25, 30, 25)
// test.kt:12 $box (12, 17, 12)
// test.kt:15 $box (16, 8)
// test.kt:8 $box (4, 14, 4)
// test.kt:17 $box (1)
