import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect("123") {
        val list = listOf("1", "2", "3", "4")
        list.reduceIndexed { index, a, b -> if (index == 3) a else a + b }
    }

    expect(5) {
        listOf(2, 3).reduceIndexed { index, acc: Number, e ->
            assertEquals(1, index)
            assertEquals(2, acc)
            assertEquals(3, e)
            acc.toInt() + e
        }
    }

    assertFailsWith<UnsupportedOperationException> {
        arrayListOf<Int>().reduceIndexed { index, a, b -> a + b }
    }
}
