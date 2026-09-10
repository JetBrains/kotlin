
fun <F> foo() {
    class LocalClass<T> {
        inner typealias LocalTypeAlias = LocalClass<T>

        fun b<caret>ar(): LocalTypeAlias? = null
    }
}
