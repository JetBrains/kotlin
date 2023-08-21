// FIR_IDENTICAL
interface Box<F> {
    fun add(f: F)
    fun get(): F
}

fun <E> myBuilder(x: Box<E>.() -> Unit): Box<E> = TODO()

fun main() {
    myBuilder {
        add("")
    }.get().length
}