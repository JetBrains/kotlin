// "Assign to property" "true"
class Test(var foo: Int) {
    fun test(foo: Int) {
        <caret>foo = foo
    }
}