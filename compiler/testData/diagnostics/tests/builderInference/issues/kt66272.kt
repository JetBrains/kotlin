// FIR_IDENTICAL
// ISSUE: KT-66272

data class DataClass(val data: String)

fun test() {
    A.create {
        it.group().apply(it, ::DataClass)
    }
}
open class A<O, F> {
    open fun group(): A<F, String> {
        return null!!
    }
    fun <R> apply(instance: A<O, *>, function: (F) -> R): A<O, R> {
        return null!!
    }
    companion object {
        fun <T> create(a: (A<T, T>) -> A<T, T>) {}
    }
}
