// "Assign to property" "true"
class Test {
    var foo = 1

    fun test(foo: Int) {
        <caret>foo = foo
    }
}