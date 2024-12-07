// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM_IR

fun <K1, V1> Map<out K1, V1>.plus(pair: Pair<K1, V1>): Map<K1, V1> =
        if (this.isEmpty()) mapOf(pair) else LinkedHashMap(this).apply { put(pair.first, pair.second) }
