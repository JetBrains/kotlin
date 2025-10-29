// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun main() {
    val it: Sequence<Long> = Sequence.of(1, 2, 3)
}
