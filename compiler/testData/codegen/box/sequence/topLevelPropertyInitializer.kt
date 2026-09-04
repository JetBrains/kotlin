// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

val doubled = sequenceOf(1, 2, 3).map { it * 2 }.take(2).toList()
val built = sequence { yield(1); yield(2) }.map { it + 1 }.toList()
val firstEven = generateSequence(1) { it + 1 }.first { it % 2 == 0 }
fun box(): String {
    return if (doubled == listOf(2, 4) && built == listOf(2, 3) && firstEven == 2) "OK" else "Fail: $doubled, $built, $firstEven"
}
