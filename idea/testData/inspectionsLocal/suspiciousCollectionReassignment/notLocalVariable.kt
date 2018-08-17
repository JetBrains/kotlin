// WITH_RUNTIME
class Test {
    var list = listOf(1)
    fun test() {
        list += 2<caret>
    }
}