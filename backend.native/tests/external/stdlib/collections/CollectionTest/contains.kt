import kotlin.test.*
import kotlin.comparisons.*

fun <T> Iterable<T>.toIterable(): Iterable<T> = Iterable { this.iterator() }

fun box() {
    assertFalse(hashSetOf<Int>().contains(12))
    assertTrue(listOf(15, 19, 20).contains(15))

    assertTrue(hashSetOf(45, 14, 13).toIterable().contains(14))
}
