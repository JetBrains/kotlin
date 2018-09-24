// "Assign to property" "true"
class Test {
    val foo: Int

    constructor(foo: Int) {
        <caret>foo = foo
    }
}