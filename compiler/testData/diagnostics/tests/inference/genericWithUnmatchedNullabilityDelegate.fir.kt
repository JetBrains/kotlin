// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-67912
// WITH_STDLIB

interface Bound

inline fun <reified F : Bound> foo(key: String): F? = null

fun main() {
    val otherValue: Map<String, String> by lazy {
        <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>foo<!>("") ?: emptyMap()
    }
}
