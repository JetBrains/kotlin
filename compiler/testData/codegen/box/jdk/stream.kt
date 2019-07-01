// JVM_TARGET: 1.8
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

import java.util.stream.*

class B<F> : List<F> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun contains(element: F): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(elements: Collection<F>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): F {
        throw UnsupportedOperationException()
    }

    override fun indexOf(element: F): Int {
        throw UnsupportedOperationException()
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterator(): Iterator<F> {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(element: F): Int {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): ListIterator<F> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): ListIterator<F> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        throw UnsupportedOperationException()
    }

    override fun stream() = Stream.of("abc", "ab") as Stream<F>
}

fun box(): String {
    val a: List<String> = listOf("abc", "a", "ab")
    val b = a.stream().filter { it.length > 1 }.collect(Collectors.toList())
    if (b != listOf("abc", "ab")) return "fail 1"

    val c = B<String>().stream().collect(Collectors.toList())
    if (c != listOf("abc", "ab")) return "fail 2"

    return "OK"
}
