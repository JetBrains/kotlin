trait A {
    val foo: Int.()->Unit

    fun test() {
        1.foo<caret>()
    }
}
