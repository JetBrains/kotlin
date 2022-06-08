// IGNORE_BACKEND: JS_IR
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

// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:5 box
// test.kt:10 box
// test.kt:11 box
// test.kt:5 box
// test.kt:6 box
// test.kt:13 box
// test.kt:5 box
// test.kt:14 box
// test.kt:15 box
// test.kt:5 box
// test.kt:6 box
// test.kt:17 box
// test.kt:5 box
// test.kt:18 box
// test.kt:20 box
// test.kt:5 box
// test.kt:21 box
// test.kt:22 box
// test.kt:5 box
// test.kt:6 box
// test.kt:23 box
// test.kt:5 box
// test.kt:6 box
// test.kt:24 box

// EXPECTATIONS JS_IR
