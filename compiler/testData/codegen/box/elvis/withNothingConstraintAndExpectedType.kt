// ISSUE: KT-73166

fun <T> bar(t: T, r: Any?): T = r as T
fun foo(): String? = bar(null, "OK") ?: bar(null, "fail")

fun box(): String {
    return foo()!!
}
