fun interface GenericPredicate<T> {
    fun test(value: T): Boolean
}

fun usage(p: <caret>GenericPredicate<String>) {}
