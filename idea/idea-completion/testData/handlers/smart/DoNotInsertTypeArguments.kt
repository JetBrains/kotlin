public inline fun <reified T> Iterable<*>.foo(): List<T> { }

fun f(list: List<Any>): Collection<String> {
    return list.<caret>
}

// ELEMENT: foo
