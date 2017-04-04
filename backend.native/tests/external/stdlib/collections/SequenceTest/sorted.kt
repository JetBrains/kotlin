import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    sequenceOf(3, 7, 5).let {
        it.sorted().iterator().assertSorted { a, b -> a <= b }
        it.sortedDescending().iterator().assertSorted { a, b -> a >= b }
    }
}
