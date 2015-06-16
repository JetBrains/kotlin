fun foo(paramFirst: Int, paramSecond: Int) = 12
fun foo(paramThird: String, paramFourth: String) = 1

fun test() {
    foo(12, param<caret>)
}

// ABSENT: paramFirst
// EXIST: {"lookupString":"paramSecond","tailText":" Int","itemText":"paramSecond ="}
// ABSENT: paramThird
// ABSENT: paramFourth