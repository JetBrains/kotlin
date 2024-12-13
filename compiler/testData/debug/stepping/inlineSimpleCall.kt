

// FILE: test.kt

inline fun inlineFun(s: () -> Unit) {
    s()
}

fun box() {
    inlineFun ({
        1
    })

    inlineFun {
        2
    }

    inlineFun {
        3

        inlineFun {
            4
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:6 box
// test.kt:11 box
// test.kt:12 box
// test.kt:6 box
// test.kt:7 box
// test.kt:14 box
// test.kt:6 box
// test.kt:15 box
// test.kt:16 box
// test.kt:6 box
// test.kt:7 box
// test.kt:18 box
// test.kt:6 box
// test.kt:19 box
// test.kt:21 box
// test.kt:6 box
// test.kt:22 box
// test.kt:23 box
// test.kt:6 box
// test.kt:7 box
// test.kt:24 box
// test.kt:6 box
// test.kt:7 box
// test.kt:25 box

// EXPECTATIONS JS_IR
// test.kt:25 box

// EXPECTATIONS WASM
// test.kt:10 $box (4)
// test.kt:6 $box (4)
// test.kt:11 $box (8)
// test.kt:12 $box (5)
// test.kt:6 $box (4)
// test.kt:7 $box (1)
// test.kt:10 $box (4)
// test.kt:14 $box (4)
// test.kt:6 $box (4)
// test.kt:15 $box (8)
// test.kt:16 $box (5)
// test.kt:6 $box (4)
// test.kt:7 $box (1)
// test.kt:14 $box (4)
// test.kt:18 $box (4)
// test.kt:6 $box (4)
// test.kt:19 $box (8)
// test.kt:21 $box (8)
// test.kt:6 $box (4)
// test.kt:22 $box (12)
// test.kt:23 $box (9)
// test.kt:6 $box (4)
// test.kt:7 $box (1)
// test.kt:21 $box (8)
// test.kt:24 $box (5)
// test.kt:6 $box (4)
// test.kt:7 $box (1)
// test.kt:18 $box (4)
// test.kt:25 $box (1)
