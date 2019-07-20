// PROBLEM: none
enum class SomeEnum {
    <caret>USED
}

fun test() {
    enumValueOf<SomeEnum>("USED")
}