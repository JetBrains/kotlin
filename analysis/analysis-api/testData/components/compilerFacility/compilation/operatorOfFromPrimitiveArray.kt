// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

fun takeLongs(vararg longs: Long) { }

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun main() {
    val it: LongArray = LongArray.of(1, 2, 3)
    takeLongs(*LongArray.of(1, 2, 3))
}
