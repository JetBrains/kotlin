// WITH_STDLIB
// ISSUE: KT-81722
// DUMP_CODE

@OptIn(kotlin.ExperimentalCollectionLiterals::class)
fun takeULongs(vararg ulong: ULong) { }

@OptIn(kotlin.ExperimentalCollectionLiterals::class, kotlin.ExperimentalUnsignedTypes::class)
fun main() {
    val it: ULongArray = ULongArray.of(1u, 2u, 3u)
    takeULongs(*ULongArray.of(1u, 2u, 3u))
}
