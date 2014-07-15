
import kotlin.test.assertEquals

fun box() : String {
    val x = LongArray(5)
    for (i in 0..4) {
        x[i] = (i + 1).toLong()
    }

    assertEquals(15L, x.fold(0L) { x, y -> x + y })

    return "OK"
}
