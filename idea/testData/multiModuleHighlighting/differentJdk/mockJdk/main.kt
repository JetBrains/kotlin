package foo
import java.io.File

class Jdk6List<F> : List<F> {
    override val size: Int
        get() = null!!

    override fun contains(element: F): Boolean {
        null!!
    }

    override fun containsAll(elements: Collection<F>): Boolean {
        null!!
    }

    override fun get(index: Int): F {
        null!!
    }

    override fun indexOf(element: F): Int {
        null!!
    }

    override fun isEmpty(): Boolean {
        null!!
    }

    override fun iterator(): Iterator<F> {
        null!!
    }

    override fun lastIndexOf(element: F): Int {
        null!!
    }

    override fun listIterator(): ListIterator<F> {
        null!!
    }

    override fun listIterator(index: Int): ListIterator<F> {
        null!!
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        null!!
    }
}

fun buildList(): List<String> = null!!
fun myFile(): File = null!!

fun mainJdk6(x: List<String>) {
    x.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: stream">stream</error>().<error descr="[DEBUG] Resolved to error element">filter</error> { <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: it">it</error>.<error descr="[DEBUG] Resolved to error element">length</error> <error descr="[DEBUG] Resolved to error element">></error> 0 }
}
