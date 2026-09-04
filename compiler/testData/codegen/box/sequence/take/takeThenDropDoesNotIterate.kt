// WITH_STDLIB
var calls = 0
fun source() = generateSequence(1) { calls++; it + 1 }.map { calls++; it }
fun check(expectedCalls: Int, block: () -> List<Int>, expected: List<Int>): String {
    calls = 0
    val r = block()
    return if (r == expected && calls == expectedCalls) "" else "Fail: $r calls=$calls; "
}
fun box(): String {
    val r = check(0, { source().take(1).drop(1).toList() }, listOf()) +
            check(0, { source().take(3).drop(1).drop(2).toList() }, listOf()) +
            check(0, { source().take(5).take(2).drop(2).toList() }, listOf()) +
            check(0, { source().drop(1).take(2).drop(2).toList() }, listOf()) +
            check(5, { source().take(3).drop(1).toList() }, listOf(2, 3)) +
            check(5, { source().take(3).filter { true }.drop(3).toList() }, listOf())
    return if (r.isEmpty()) "OK" else r
}
