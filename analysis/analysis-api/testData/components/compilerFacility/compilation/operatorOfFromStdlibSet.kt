// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun main() {
    val it: Set<Long> = Set.of(1, 2, 3)
}
