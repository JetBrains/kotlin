// PROBLEM: Variable 'foo' is assigned to itself
// FIX: Remove self assignment

class Test {
    var foo = 1

    fun test() {
        foo = <caret>this.foo
    }
}