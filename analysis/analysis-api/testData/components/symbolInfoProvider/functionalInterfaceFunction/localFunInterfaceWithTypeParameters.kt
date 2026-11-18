fun usage() {
    fun interface GenericPredicate<T> {
        fun test(value: T): Boolean
    }

    fun check(p: <caret>GenericPredicate<String>) {}
}
