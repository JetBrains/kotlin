// PROBLEM: none
// WITH_RUNTIME
fun test() {
    emptyList<String>().<caret>mapIndexed(fun(index: Int, value: String) {
        println(value)
        index
    })
}