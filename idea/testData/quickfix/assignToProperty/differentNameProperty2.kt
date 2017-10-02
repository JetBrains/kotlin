// "Assign to property" "false"
// ERROR: Val cannot be reassigned
class Test(var bar: Int) {
    fun test(foo: Int) {
        <caret>foo = foo
    }
}