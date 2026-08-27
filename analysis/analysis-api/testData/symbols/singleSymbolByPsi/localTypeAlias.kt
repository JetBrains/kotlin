class MyClass<A> {
    fun <B> check() {
        type<caret>alias MyAlias<C, D> = Map<Map<A, B>, Map<C, D>>
    }
}
