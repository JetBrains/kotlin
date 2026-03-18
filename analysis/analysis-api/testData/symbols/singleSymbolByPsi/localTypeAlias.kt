// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

class MyClass<A> {
    fun <B> check() {
        type<caret>alias MyAlias<C, D> = Map<Map<A, B>, Map<C, D>>
    }
}
