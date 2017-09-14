open class Foo {
    open fun arguments(arg1: Int, arg2: Long) {
    }
}

class Bar : Foo() {
    override <caret>fun arguments(arg1: Int, arg2: Long) {
        super.arguments(arg1, arg2)
    }
}
