// !WITH_NEW_INFERENCE

fun <T> foo(resources: List<T>) {
    resources.map { runCatching { it } }.mapNotNull { it.getOrNull() }
}

fun <T: Any> bar(resources: List<T>) {
    resources.map { runCatching { it } }.mapNotNull { it.getOrNull() }
}