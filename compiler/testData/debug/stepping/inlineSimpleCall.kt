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

// LINENUMBERS
// test.kt:8 box
// test.kt:4 box
// test.kt:9 box
// test.kt:10 box
// test.kt:5 box
// test.kt:12 box
// test.kt:4 box
// test.kt:13 box
// test.kt:14 box
// test.kt:5 box
// test.kt:16 box
// test.kt:4 box
// test.kt:17 box
// test.kt:19 box
// test.kt:4 box
// test.kt:20 box
// test.kt:21 box
// test.kt:5 box
// test.kt:22 box
// test.kt:5 box
// test.kt:23 box
