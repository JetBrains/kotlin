//!DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// !LANGUAGE: -NonStrictOnlyInputTypesChecks

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.jvm.JvmName("containsAny")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <T> Iterable<T>.contains1(element: T): Int = null!!

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.contains1(element: T): Boolean = null!!

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@JvmName("getAny")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <K, V> Map<K, V>.get1(key: Any?): Int = null!!

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.get1(key: K): V? = null!!

fun test(map: Map<Int, String>) {
    val a: Int = listOf(1).contains1("")
    val b: Boolean = listOf(1).contains1(1)

    val c: String? = map.get1("")
    val d: String? = map.get1(1)
}