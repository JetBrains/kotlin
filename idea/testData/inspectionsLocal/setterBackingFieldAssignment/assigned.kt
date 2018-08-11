// PROBLEM: none
class Test {
    var foo: Int = 1
        <caret>set(value) {
            bar()
            field = value
        }

    fun bar() {}
}