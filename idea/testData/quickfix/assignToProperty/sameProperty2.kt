// "Assign to property" "true"
class Test {
    var foo = 1

    fun test(foo: String) {
        <caret>foo = 2
    }
}