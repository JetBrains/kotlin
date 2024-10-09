fun List<String>.filter(predicate: (String) -> Boolean): List<String> {
    return this
}

fun usage(items: List<String>) {
    items.filter { item -> true }
}