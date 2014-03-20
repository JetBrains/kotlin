fun foo(paramFirst: String, paramSecond: String, paramThird: String, paramFourth: String) {}

fun test() {
    foo("str", paramThird = "test", param<caret>)
}

// ABSENT: paramFirst
// EXIST: paramSecond
// ABSENT: paramThird
// EXIST: paramFourth

