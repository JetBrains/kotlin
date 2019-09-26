class A {
    init {
        val foo: String<caret>
        bar()
        foo = ""
    }

    fun bar() {}
}