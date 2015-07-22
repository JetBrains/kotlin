// IS_APPLICABLE: false
// WITH_RUNTIME
fun b(c: List<String>) {
    for ((<caret>indexVariable, d) in c.withIndex()) {

    }
}