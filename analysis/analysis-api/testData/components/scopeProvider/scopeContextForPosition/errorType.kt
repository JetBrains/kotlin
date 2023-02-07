interface List<T> {
    fun add(e: T)
    fun get(index: Int): T
}

inline fun <E> buildList(f: List<E>.() -> Unit): List<E> = l

fun test() {
    buildList {
        with(this.get(0)) {
            <expr>e</expr>
        }
    }
}