import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect("234") {
        val list = listOf("1", "2", "3", "4")
        list.reduceRightIndexed { index, a, b -> if (index == 0) b else a + b }
    }

    expect(1) {
        listOf(2, 3).reduceRightIndexed { index, e, acc: Number ->
            assertEquals(0, index)
            assertEquals(3, acc)
            assertEquals(2, e)
            acc.toInt() - e
        }
    }

    assertFailsWith<UnsupportedOperationException> {
        arrayListOf<Int>().reduceRightIndexed { index, a, b -> a + b }
    }
}
