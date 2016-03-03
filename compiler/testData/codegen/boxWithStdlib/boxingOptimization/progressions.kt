
import kotlin.test.assertEquals

fun box() : String {

    val result1 = (1..100).count { x -> x % 2 == 0 }
    val result2 = (1..100).filter { x -> x % 2 == 0 }.size
    assertEquals(result1, 50)
    assertEquals(result2, 50)

    val result3 = (1..100).map { x -> 2 * x }.count { x -> x % 2 == 0 }
    val result4 = (1..100).map { x -> 2 * x }.filter { x -> x % 2 == 0 }.size
    assertEquals(result3, 100)
    assertEquals(result4, 100)

    val result5 = (1L..100L).count { x -> x % 2 == 0L }
    val result6 = (1L..100L).filter { x -> x % 2 == 0L }.size
    assertEquals(result5, 50)
    assertEquals(result6, 50)

    val result7 = (1L..100L).map { x -> 2 * x }.count { x -> x % 2 == 0L }
    val result8 = (1L..100L).map { x -> 2 * x }.filter { x -> x % 2 == 0L }.size
    assertEquals(result7, 100)
    assertEquals(result8, 100)

    val result9 = (0..10).reduce { total, next -> total + next }
    val result10 = (0L..10L).reduce { total, next -> total + next }
    assertEquals(result9, 55)
    assertEquals(result10, 55L)

    return "OK"
}

