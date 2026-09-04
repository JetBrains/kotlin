// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

class C(val items: List<Int>) {
    val doubled = items.asSequence().map { it * 2 }.toList()
    val built = sequence { yieldAll(items) }.filter { it > 1 }.toList()
    var count = 0
    init {
        for (x in items.asSequence().take(1)) count += x
    }
}
object O {
    val r = sequenceOf(1, 2).map { it * 3 }.toList()
}
fun box(): String {
    val c = C(listOf(1, 2))
    return if (c.doubled == listOf(2, 4) && c.built == listOf(2) && c.count == 1 && O.r == listOf(3, 6)) "OK"
    else "Fail: ${c.doubled}, ${c.built}, ${c.count}, ${O.r}"
}
