// "Remove invocation" "true"
enum class Test {
    A
}

fun test() {
    Test.A<caret>()
}