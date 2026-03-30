// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

import java.util.*

fun <K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V> = EnumMap(mapOf(*pairs))

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, nullableType, outProjection, typeConstraint,
typeParameter, vararg */
