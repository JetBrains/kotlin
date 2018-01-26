// WITH_RUNTIME

import kotlin.test.assertEquals

data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int) {
    operator fun component1() = a + 1231

    operator fun component2() = b + 2318

    operator fun deconstruct() = A(a, b)
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int): List<Int> = when (value) {
    is String -> listOf(0)
    is val m = B(val a, p2 + p3) -> listOf(1, a)
    is val m = A(val a, p2 + p3) -> listOf(2, a)
    is val m = Pair(5, 7) -> listOf(3)
    is val m = Pair(val a: Int, p1) -> listOf(4, a)
    is val m = List(Int(), Int()) ->listOf(5)
    is val m = Pair(val a: Int, val b: Int) && a > p1 -> listOf(6, a, b)
    is val m = Pair("some string $p4 with parameter", _) -> listOf(7)
    is val m = Pair(Int(), Pair(val a: Int, val b: Int)) -> listOf(8, a, b)
    is val m -> listOf(9)
}

fun box() : String {
    assertEquals(matcher("3D4R0V4", 0, 0, 0, 0), listOf(0))
    assertEquals(matcher(B(5, 6), 0, 4, 2, 0), listOf(1, 5))
    assertEquals(matcher(A(5, 6), 0, 3, 3, 0), listOf(2, 5))
    assertEquals(matcher(Pair(5, 7), 0, 0, 0, 0), listOf(3))
    assertEquals(matcher(Pair(1, 2), 2, 0, 0, 0), listOf(4, 1))
    assertEquals(matcher(listOf(1, 2, 3, 4, 5, 6, 7), 0, 0, 0, 0), listOf(5))
    assertEquals(matcher(Pair(1, 2), 0, 0, 0, 0), listOf(6, 1, 2))
    assertEquals(matcher(Pair("some string 4 with parameter", 7), 0, 0, 0, 4), listOf(7))
    assertEquals(matcher(Pair(1, Pair(3, 9)), 0, 0, 0, 0), listOf(8, 3, 9))
    assertEquals(matcher(10, 0, 0, 0, 0), listOf(9))
    return "OK"
}
