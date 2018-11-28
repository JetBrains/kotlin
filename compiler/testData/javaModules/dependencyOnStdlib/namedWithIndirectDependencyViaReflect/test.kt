import kotlin.text.Regex

fun f1(): List<String?> = emptyList()
fun f2(): Array<Lazy<Unit>> = arrayOf()
fun f3(map: Map<Int, Regex>): Collection<Regex> =
        map.filterNot { (key, entry) -> "$key".equals(entry.toString(), ignoreCase = true) }.values
