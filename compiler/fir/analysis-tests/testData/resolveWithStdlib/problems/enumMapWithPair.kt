// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// ISSUE: KT-68677
// LANGUAGE: +StricterConstraintIncorporationRecursionDetector

import java.util.*

class CustomEnumMap<KK : Enum<KK>, VV> private constructor(
    private val enumMap: EnumMap<KK, VV>
) : Map<KK, VV> by enumMap {

    constructor(p: Pair<KK, VV>) : this(EnumMap(mapOf(p)))
}

fun <KKK, VVV> mapOf(p: Pair<KKK, VVV>): Map<KKK, VVV> = TODO()
