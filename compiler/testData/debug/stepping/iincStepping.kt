//FILE: test.kt
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

// IGNORE_BACKEND: JVM
// The current backend has strange stepping behavior for assignments.
// It generates the line number for the assignment first, and then
// the evaluation of the right hand side with line numbers.
// That leads to the line number with the assignment typically
// not being hit at all as it has no instructions. Also, stepping
// through the evaluation of the right hand side and then hitting
// the line number for the actual assignment makes more sense as
// that is the actual evaluation order.

// LINENUMBERS
// test.kt:3
// test.kt:4
// test.kt:5
// test.kt:6
// test.kt:7
// test.kt:6
// test.kt:8
// test.kt:9
// test.kt:10
// test.kt:9
// test.kt:11
// test.kt:13
// test.kt:12
// test.kt:15
// test.kt:16
// test.kt:15
// test.kt:14
// test.kt:17