data class A(val a: Int, val b: Int)

class B(val a: Int, val b: Int) {
    fun deconstruct() = A(a, b)
}

fun matcher(value: Any?, p1: Int, p2: Int, p3: Int, p4: Int): List<Int> = when (value) {
    is String -> listOf(0)
    match m @ B(a, #(p2 + p3)) -> listOf(1, a)
    match m @ A(a, #(p2 + p3)) -> listOf(2, a)
    match m @ Pair<*, *>(5, 7) -> listOf(3)
    match m @ Pair<*, *>(a: Int, #p1) -> listOf(4, a)
    match m @ List<*>(:Int, :Int) ->listOf(5)
    match m @ Pair<*, *>(a: Int, b: Int) if (a > p1) -> listOf(6, a, b)
    match m @ Pair<*, *>("some string $p4 with parameter", _) -> listOf(7)
    match m @ Pair<*, *>(:Int, Pair<*, *>(a: Int, b: Int)) -> listOf(8, a, b)
    match m -> listOf(9)
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun matcher(p: Any?) = when (p) {
    is String -> listOf(0)
    match Pair<*, *>(a, Pair<*, *>(b, c)) -> listOf(1, a, b, c)
    else -> listOf(2)
}

fun matcher(value: Any?) = when (value) {
    match Pair<*, *>(a: Pair<*, *> @ Pair<*, *>(b: Int, _), :Int) -> listOf(0, a, b)
    match x -> listOf(1, x)
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun box() : String {
    val a = Pair(1, 2)
    when (a) {
        match (_, d) -> {
            assertEquals(d, 2)
            return "OK"
        }
        else -> return "match fail"
    }
    return "fail when generation"
}
