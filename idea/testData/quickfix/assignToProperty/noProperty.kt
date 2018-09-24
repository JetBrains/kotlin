// "Assign to property" "false"
// ERROR: Val cannot be reassigned
class Test(foo: Int) {
    fun test(foo: Int) {
        <caret>foo = foo
    }
}