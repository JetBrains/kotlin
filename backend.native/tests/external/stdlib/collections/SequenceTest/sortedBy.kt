import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    sequenceOf("it", "greater", "less").let {
        it.sortedBy { it.length }.iterator().assertSorted { a, b -> compareValuesBy(a, b) { it.length } <= 0 }
        it.sortedByDescending { it.length }.iterator().assertSorted { a, b -> compareValuesBy(a, b) { it.length } >= 0 }
    }

    sequenceOf('a', 'd', 'c', null).let {
        it.sortedBy { it }.iterator().assertSorted { a, b -> compareValues(a, b) <= 0 }
        it.sortedByDescending { it }.iterator().assertSorted { a, b -> compareValues(a, b) >= 0 }
    }
}
