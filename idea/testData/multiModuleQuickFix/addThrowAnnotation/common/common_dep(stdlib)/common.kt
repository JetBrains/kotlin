// "Add '@Throws' annotation" "true"

class FooException : Exception()

fun test() {
    <caret>throw FooException()
}