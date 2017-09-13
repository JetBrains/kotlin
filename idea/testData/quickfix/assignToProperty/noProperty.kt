// "Assign to property" "false"
// ACTION: Remove redundant assignment
// ERROR: Val cannot be reassigned
class Test {
    var bar = 1

    fun test(foo: Int) {
        <caret>foo = foo
    }
}