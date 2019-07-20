// PROBLEM: none
enum class SomeEnum {
    <caret>USED
}

fun test() {
    enumValues<SomeEnum>()
}