// WITH_RUNTIME
fun List<String>.test() {
    <caret>mapIndexed(fun(index: Int, _: String): Int {
        return index
    })
}