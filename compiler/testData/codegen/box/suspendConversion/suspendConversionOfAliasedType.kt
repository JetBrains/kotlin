// DONT_TARGET_EXACT_BACKEND: JVM
// ISSUE: KT-65002
typealias Listener = (String) -> Unit

fun foo(f: suspend (String) -> Unit): String = "OK"

fun box(): String {
    val f: Listener = {}
    return foo(f)
}
