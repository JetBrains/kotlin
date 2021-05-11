// WITH_RUNTIME
// IGNORE_BACKEND: WASM

class C : Comparable<C> {
    override fun compareTo(other: C): Int = 0
}

fun box(): String {
    val comparator = Comparable<C>::compareTo
    return if (nullsFirst(comparator).compare(C(), C()) == 0) "OK" else "Fail"
}
