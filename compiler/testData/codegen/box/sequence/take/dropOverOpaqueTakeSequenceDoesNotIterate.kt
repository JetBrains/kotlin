// WITH_STDLIB
// `drop` of a `TakeSequence` returns an empty sequence without iterating when everything is dropped,
// which can not be known for the sequence of a variable or of a parameter
var calls = 0
fun two() = 2
fun dropAll(s: Sequence<Int>) = s.drop(5).toList()
fun box(): String {
    val t = generateSequence(1) { calls++; it + 1 }.take(two())
    val r1 = t.drop(5).toList()
    val r2 = dropAll(generateSequence(1) { calls++; it + 1 }.map { calls++; it }.take(3))
    val r3 = dropAll(generateSequence(1) { calls++; it + 1 }.take(3).drop(1))
    if (r1.isNotEmpty() || r2.isNotEmpty() || r3.isNotEmpty() || calls != 0) return "Fail: $r1 $r2 $r3 calls=$calls"
    var n = 0
    val once = generateSequence { if (n < 3) ++n else null }.take(two())
    val r4 = once.drop(5).toList()
    val r5 = once.toList()
    return if (r4.isEmpty() && r5 == listOf(1, 2)) "OK" else "Fail: $r4 $r5"
}
