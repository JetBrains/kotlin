// WITH_STDLIB

// FILE: test.kt
fun box() {
    var i = 0
    do {
        println("body: $i")
    }
    while(
        try {
           i++
        }
        finally {
            println("finally: $i")
        } < 2
    )
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:7 box
// test.kt:10 box
// test.kt:11 box
// test.kt:14 box
// test.kt:15 box
// test.kt:14 box
// test.kt:15 box
// test.kt:10 box
// test.kt:7 box
// test.kt:10 box
// test.kt:11 box
// test.kt:14 box
// test.kt:15 box
// test.kt:14 box
// test.kt:15 box
// test.kt:10 box
// test.kt:7 box
// test.kt:10 box
// test.kt:11 box
// test.kt:14 box
// test.kt:15 box
// test.kt:14 box
// test.kt:15 box
// test.kt:10 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:7 box
// test.kt:11 box
// test.kt:11 box
// test.kt:11 box
// test.kt:14 box
// test.kt:10 box
// test.kt:7 box
// test.kt:11 box
// test.kt:11 box
// test.kt:11 box
// test.kt:14 box
// test.kt:10 box
// test.kt:7 box
// test.kt:11 box
// test.kt:11 box
// test.kt:11 box
// test.kt:14 box
// test.kt:10 box
// test.kt:17 box

// EXPECTATIONS WASM
// test.kt:5 $box (12)
// test.kt:7 $box (17, 24, 16, 8)
// test.kt:11 $box (11, 12, 11)
// test.kt:14 $box (12, 21, 31, 20, 12)
// test.kt:15 $box (12)
// test.kt:10 $box (8)
// test.kt:7 $box (17, 24, 16, 8)
// test.kt:11 $box (11, 12, 11)
// test.kt:14 $box (12, 21, 31, 20, 12)
// test.kt:15 $box (12)
// test.kt:10 $box (8)
// test.kt:7 $box (17, 24, 16, 8)
// test.kt:11 $box (11, 12, 11)
// test.kt:14 $box (12, 21, 31, 20, 12)
// test.kt:15 $box (12)
// test.kt:10 $box (8)
// test.kt:17 $box (1)

// EXPECTATIONS NATIVE
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:11 box
// test.kt:10 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:10 box
// test.kt:6 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:11 box
// test.kt:10 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:10 box
// test.kt:6 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:7 box
// test.kt:11 box
// test.kt:10 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:10 box
// test.kt:17 box
