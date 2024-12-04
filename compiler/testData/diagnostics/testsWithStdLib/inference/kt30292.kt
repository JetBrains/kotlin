// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun test(ls: List<String>) {
    ls.takeIf(Collection<*>::isNotEmpty)
}