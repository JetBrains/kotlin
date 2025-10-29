// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun main() {
    val it: Array<Long> = Array.of(1, 2, 3)
    listOf(*Array.of(1, 2, 3))
}
