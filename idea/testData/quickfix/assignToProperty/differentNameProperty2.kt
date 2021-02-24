// "Assign to property" "false"
// ACTION: Convert assignment to assignment expression
// ERROR: Val cannot be reassigned
class Test(var bar: Int) {
    fun test(foo: Int) {
        <caret>foo = foo
    }
}