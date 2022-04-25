// FIR_IDENTICAL
// WITH_STDLIB
// FULL_JDK

import java.util.*

fun <K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V> = EnumMap(mapOf(*pairs))
