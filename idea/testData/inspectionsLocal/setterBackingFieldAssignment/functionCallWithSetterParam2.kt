// PROBLEM: none
class Test {
    var foo: Int = 10
        <caret>set(value: Int) {
            bar(value = value)
        }

    fun bar(value: Int) {}
}