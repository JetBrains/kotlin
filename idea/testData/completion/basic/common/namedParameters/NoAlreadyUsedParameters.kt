fun foo(paramFirst: String, paramSecond: String, paramThird: String, paramFourth: String) {}

fun test() {
    foo("str", paramThird = "test", param<caret>)
}

// ABSENT: {"lookupString":"paramFirst = ","itemText":"paramFirst = "}
// EXIST: {"lookupString":"paramSecond = ","itemText":"paramSecond = "}
// ABSENT: {"lookupString":"paramThird = ","itemText":"paramThird = "}
// EXIST: {"lookupString":"paramFourth = ","itemText":"paramFourth = "}
