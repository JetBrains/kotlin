// PROBLEM: none
enum class SomeEnum {
    <caret>USED
}

fun test() {
    val e: SomeEnum = enumValueOf("USED")
}