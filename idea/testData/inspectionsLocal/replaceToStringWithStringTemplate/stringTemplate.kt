// WITH_RUNTIME
// PROBLEM: none
fun test(): String {
    val x = 1
    return "Foo: ${x.<caret>toString()}"
}