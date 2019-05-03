class User : AnnotatedDerived() {
    fun test() {
        val x = foo("123")
        val y = foo(null)
    }
}