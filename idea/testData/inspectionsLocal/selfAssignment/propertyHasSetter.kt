// PROBLEM: none
// WITH_RUNTIME

class Test {
    var foo = 1
        set(value) {
            println(value)
        }

    fun test() {
        foo = <caret>foo
    }
}
