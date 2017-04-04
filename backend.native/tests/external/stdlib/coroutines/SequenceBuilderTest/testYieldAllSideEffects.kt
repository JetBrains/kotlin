import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val effects = arrayListOf<Any>()
    val result = buildSequence {
        effects.add("a")
        yieldAll(listOf(1, 2))
        effects.add("b")
        yieldAll(listOf())
        effects.add("c")
        yieldAll(listOf(3))
        effects.add("d")
        yield(4)
        effects.add("e")
        yieldAll(listOf())
        effects.add("f")
        yield(5)
    }

    for (res in result) {
        effects.add("(") // marks step start
        effects.add(res)
        effects.add(")") // marks step end
    }
    assertEquals(
            listOf(
                    "a",
                    "(", 1, ")",
                    "(", 2, ")",
                    "b", "c",
                    "(", 3, ")",
                    "d",
                    "(", 4, ")",
                    "e", "f",
                    "(", 5, ")"
            ),
            effects.toList()
    )
}
