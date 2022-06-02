// FIR_IDENTICAL

fun test(ls: List<String>) {
    ls.takeIf(Collection<*>::isNotEmpty)
}