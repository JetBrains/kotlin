// QUERY: annotations

class Foo<T> {
    @T
    @Unresolved
    @Int
    fun fo<caret>o() {

    }
}
