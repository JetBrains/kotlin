// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JVM
// FILE: test.kt
fun box() {
    var i = 0
    ++i
    i += 1
    i +=
        1
    i -= 1
    i -=
        1
    i = i + 1
    i =
        i + 1
    i =
        i +
            1
}

// The current backend has strange stepping behavior for assignments.
// It generates the line number for the assignment first, and then
// the evaluation of the right hand side with line numbers.
// That leads to the line number with the assignment typically
// not being hit at all as it has no instructions. Also, stepping
// through the evaluation of the right hand side and then hitting
// the line number for the actual assignment makes more sense as
// that is the actual evaluation order.

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:8 box
// test.kt:10 box
// test.kt:11 box
// test.kt:12 box
// test.kt:11 box
// test.kt:13 box
// test.kt:15 box
// test.kt:14 box
// test.kt:17 box
// test.kt:18 box
// test.kt:17 box
// test.kt:16 box
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box
// test.kt:15 box
// test.kt:17 box
// test.kt:19 box