// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

@OptIn(kotlin.ExperimentalCollectionLiterals::class)
fun main() {
    val it: Array<Long> = Array.of(1, 2, 3)
    listOf(*Array.of(1, 2, 3))
}
