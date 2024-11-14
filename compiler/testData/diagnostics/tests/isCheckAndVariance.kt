// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-7972
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

private fun <E> List<E>.addAnything(element: E) {
    if (this is MutableList<E>) {
        this.add(element)
    }
}

fun oops() {
    arrayListOf(1, 2).addAnything("string")
}
