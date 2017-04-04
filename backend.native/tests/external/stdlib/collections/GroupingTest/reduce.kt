import kotlin.test.*

inline fun <T, K : Comparable<K>> maxOfBy(a: T, b: T, keySelector: (T) -> K) = if (keySelector(a) >= keySelector(b)) a else b

fun box() {
    val elements = listOf("foo", "bar", "flea", "zoo", "biscuit")
    fun Char.isVowel() = this in "aeiou"
    fun String.countVowels() = count(Char::isVowel)
    val maxVowels = elements.groupingBy { it.first() }.reduce { k, a, b -> maxOfBy(a, b, String::countVowels) }

    assertEquals(mapOf('f' to "foo", 'b' to "biscuit", 'z' to "zoo"), maxVowels)

    val elements2 = listOf("bar", "z", "fork")
    val concats = elements2.groupingBy { it.first() }.reduceTo(HashMap(maxVowels)) { k, acc, e -> acc + e }

    assertEquals(mapOf('f' to "foofork", 'b' to "biscuitbar", 'z' to "zooz"), concats)
}
