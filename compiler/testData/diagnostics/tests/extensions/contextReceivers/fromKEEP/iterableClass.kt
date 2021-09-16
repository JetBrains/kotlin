// !LANGUAGE: +ContextReceivers

typealias IterableClass<C, T> = (C) -> Iterator<T>

context(IterableClass<C, T>)
fun <C, T> C.iterator(any: Any?): Iterator<T> = this@IterableClass.invoke(this)

fun <T> listOf(vararg items: T): List<T> = null!!

fun test() {
    val f: IterableClass<List<Int>, Int> = {
        it.listIterator()
    }
    with(f) {
        listOf(1, 2, 3).iterator(null)
    }
    listOf(1, 2, 3).<!NO_CONTEXT_RECEIVER!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>iterator<!>(null)<!>
}