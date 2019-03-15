// PROBLEM: none

class Test {
    fun test() {
        <caret>Companion.foo
    }
}
class Companion {
    companion object {
        val foo = ""
    }
}