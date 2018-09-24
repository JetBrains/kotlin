// WITH_RUNTIME
fun foo() {}

fun test() {
    <caret>for (l in listOf(1, 2)) {
        foo(); foo() // comment
    }
}
