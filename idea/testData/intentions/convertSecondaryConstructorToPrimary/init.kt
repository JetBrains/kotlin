class ConvertToInit {
    fun foo() {}

    fun bar() {}

    constructor(<caret>) {
        foo()
        bar()
    }
}