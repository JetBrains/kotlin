// WITH_STDLIB

fun <T> checkTransitiveComparator(list: List<T>, comparator: Comparator<T>) {
    class Wrapper(val item: T) : Comparable<Wrapper> {
        override fun toString(): String = item.toString()

        override fun compareTo(other: Wrapper): Int {
            return comparator.compare(this.item, other.item)
        }
    }
    checkTransitiveComparator(<expr>list.map { Wrapper(it) }</expr>)
}

fun <T : Comparable<T>> checkTransitiveComparator(list: List<T>) {
}
