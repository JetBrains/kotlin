// INTENTION_TEXT: "Add '@Throws' annotation"
// WITH_RUNTIME

class FooException : Exception()

fun test() {
    <caret>throw FooException()
}