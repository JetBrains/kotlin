// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// ISSUE: KT-68677
// LANGUAGE: +StricterConstraintIncorporationRecursionDetector

import java.util.*

class CustomEnumMap<K : Enum<K>, V> private constructor(
    private val enumMap: EnumMap<K, V>
) : Map<K,V> by enumMap {

    constructor(p: Pair<K, V>) : this(EnumMap(mapOf(p)))
}
