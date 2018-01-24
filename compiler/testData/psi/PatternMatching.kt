
data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int) {
    fun deconstruct() = A(a, b)
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
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun matcher(p: Any?) = when (p) {
    is String -> listOf(0)
    is Pair(val a, Pair(val b, val c)) -> listOf(1, a, b, c)
    else -> listOf(2)
}

fun matcher(value: Any?) = when (value) {
    is Pair(val a = Pair<*, *>(val b: Int, _), Int()) -> listOf(0, a, b)
    is val x -> listOf(1, x)
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun box() : String {
    val a = Pair(1, 2)
    when (a) {
        is (_, val d) -> {
            assertEquals(d, 2)
            return "OK"
        }
        else -> return "match fail"
    }
    return "fail when generation"
}

fun matcher(any: Any?, y: Any?) = when(any) {
    is eq y -> "is eq y"
    is String -> "is String"
    !is Pair(1, _), !is Pair(_, 2) -> "!is Pair(1, _), !is Pair(_, 2)"
    is Pair(_, !eq 2) -> throw java.lang.UnsupportedOperationException("unexpected case")
    is Pair(1, 2) -> "is Pair(1, 2)"
    else -> throw java.lang.UnsupportedOperationException("unexpected case")
}
