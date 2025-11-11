// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

@OptIn(kotlin.ExperimentalCollectionLiterals::class)
fun main() {
    val it: Sequence<Long> = Sequence.of(1, 2, 3)
}
