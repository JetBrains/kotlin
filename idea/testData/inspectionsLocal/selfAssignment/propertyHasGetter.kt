// PROBLEM: none
// WITH_RUNTIME

class Test {
    var foo = 1
        get() {
            println()
            return 2
        }

    fun test() {
        foo = <caret>foo
    }
}