// !LANGUAGE: +NewInference

fun test(ls: List<String>) {
    ls.takeIf(Collection<*>::isNotEmpty)
}