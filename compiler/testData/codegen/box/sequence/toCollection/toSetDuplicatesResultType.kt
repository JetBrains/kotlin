// WITH_STDLIB
// TARGET_BACKEND: JVM

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val s = sequenceOf(1, 3).map { it % 2 }.toSet()
    if (s !is java.util.LinkedHashSet<*>) return "Fail: ${s.javaClass}"
    (s as MutableSet<Int>).add(2)
    return if (s == setOf(1, 2)) "OK" else "Fail: $s"
}
