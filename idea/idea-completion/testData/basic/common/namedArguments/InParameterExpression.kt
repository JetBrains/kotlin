val paramTest = 12

fun small(paramFirst: Int, paramSecond: Int) {
}

fun test() = small(paramFirst = param<caret>)

// EXIST: paramTest
// ABSENT: {"lookupString":"paramFirst","tailText":" Int","itemText":"paramFirst ="}
// ABSENT: paramSecond
// NOTHING_ELSE
