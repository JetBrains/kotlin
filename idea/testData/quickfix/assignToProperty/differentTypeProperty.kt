// "Assign to property" "false"
// ACTION: Convert assignment to assignment expression
// ERROR: Val cannot be reassigned
class Test {
    var foo = "1"

    fun test(foo: Int) {
        <caret>foo = foo
    }
}