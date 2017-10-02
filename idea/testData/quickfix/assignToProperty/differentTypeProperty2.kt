// "Assign to property" "false"
// ERROR: Val cannot be reassigned
class Test(var foo: String) {
    fun test(foo: Int) {
        <caret>foo = foo
    }
}