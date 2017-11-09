// !DUMP_DEPENDENCIES
// WITH_RUNTIME
// FULL_JDK

fun <K, V> Map<out K, V>.plus(pair: Pair<K, V>): Map<K, V> =
        if (this.isEmpty()) mapOf(pair) else LinkedHashMap(this).apply { put(pair.first, pair.second) }