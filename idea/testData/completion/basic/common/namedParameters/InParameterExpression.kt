val paramTest = 12

fun small(paramFirst: Int, paramSecond: Int) {
}

fun test() = small(paramFirst = <caret>)

// EXIST: paramTest
// ABSENT: paramFirst, paramSecond