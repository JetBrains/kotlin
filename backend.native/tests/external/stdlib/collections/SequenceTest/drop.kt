import kotlin.test.*

import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
}

fun box() {
    assertEquals(emptyList(), emptySequence<Int>().drop(1).toList())
    listOf(2, 3, 4, 5).let { assertEquals(it, it.asSequence().drop(0).toList()) }
    assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(7).joinToString(limit = 10))
    assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(3).drop(4).joinToString(limit = 10))
    assertFailsWith<IllegalArgumentException> { fibonacci().drop(-1) }
}
