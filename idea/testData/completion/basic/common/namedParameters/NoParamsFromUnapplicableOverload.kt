fun foo(paramFirst: Int, paramSecond: Int) = 12
fun foo(paramThird: String, paramFourth: String) = 1

fun test() {
    foo(12, param<caret>)
}

// ABSENT: {"lookupString":"paramFirst = ","itemText":"paramFirst = "}
// EXIST: {"lookupString":"paramSecond = ","itemText":"paramSecond = "}
// ABSENT: {"lookupString":"paramThird = ","itemText":"paramThird = "}
// ABSENT: {"lookupString":"paramFourth = ","itemText":"paramFourth = "}