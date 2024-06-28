// See: KTIJ-21506
// WITH_STDLIB

fun <K> List<Pair<Int, Int>>.groupBy(
    keySelector: (Pair<Int, Int>) -> K,
    valueTransform: (Pair<Int, Int>) -> Int,
): Map<K, List<Int>> = mapOf() // The result is irrelevant for the test.

fun test(nums: IntArray) {
    <expr>nums.withIndex().groupBy({ (_, value) -> value }) { (idx, _) -> idx }</expr>
}