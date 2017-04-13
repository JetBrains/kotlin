import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    class TestCollection<out E>(val data: Collection<E>) : AbstractCollection<E>() {
        override val size get() = data.size
        override fun iterator() = data.iterator()

        public override fun toArray(): Array<Any?> = super.toArray()
        public override fun <T> toArray(array: Array<T>): Array<T> = super.toArray(array)
    }

    val data = listOf(1, 2, 3)
    assertEquals(TestCollection(data).toArray().asList(), data)
    assertEquals(TestCollection(listOf<Int>()).toArray().size, 0)

    var arr1 = Array<Int>(3) { -1 }
    var arr2 = TestCollection(data).toArray(arr1)
    assertTrue(arr1 === arr2)
    assertEquals(arr2.asList(), data)

    arr1 = Array<Int>(4) { -1 }
    arr2 = TestCollection(data).toArray(arr1)
    assertTrue(arr1 === arr2)
    assertEquals(arr2.asList(), data + listOf(-1))

    arr1 = Array<Int>(2) { -1 }
    arr2 = TestCollection(data).toArray(arr1)
    assertFalse(arr1 === arr2)
    assertEquals(arr2.asList(), data)

}
