import kotlin.test.*

fun box() {
    val primitiveArray: LongArray = longArrayOf(1, 2, Long.MAX_VALUE)
    val genericArray: Array<Long> = primitiveArray.toTypedArray()
    expect(3) { genericArray.size }
    assertEquals(primitiveArray.asList(), genericArray.asList())
}
