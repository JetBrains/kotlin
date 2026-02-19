// IGNORE_FE10

fun foo() {
    class LocalClass<T> {
        inner typealias LocalTypeAlias = LocalClass<T>

        fun b<caret>ar(): LocalTypeAlias? = null
    }
}
