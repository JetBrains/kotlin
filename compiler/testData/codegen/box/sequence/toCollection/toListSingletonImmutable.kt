// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun mutability(c: Collection<Int>): String =
    if (runCatching { (c as MutableCollection<Int>).add(0) }.isSuccess) "mutable" else "immutable"

fun box(): String {
    val results = mutableListOf<String>()
    for (n in 0..2) {
        val list = (1..n).asSequence().map { it }.toList()
        val set = (1..n).asSequence().map { it }.toSet()
        results += "$n: ${mutability(list)} ${mutability(set)}"
    }
    val emptyIdentity = sequenceOf<Int>().filter { it > 0 }.toList() === emptyList<Int>()
    val r = results.joinToString() + " " + emptyIdentity
    val expected = "0: immutable immutable, 1: immutable immutable, 2: mutable mutable true"
    return if (r == expected) "OK" else "Fail: $r"
}
