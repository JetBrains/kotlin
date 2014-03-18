val paramTest = 12

fun small(paramFirst: Int, paramSecond: Int) {
}

fun test() = small(param<caret>First = 12)

// EXIST: paramFirst
// EXIST: paramSecond
// ABSENT: paramTest

// NUMBER: 2