// !MARK_DYNAMIC_CALLS


fun Any?.staticExtension() = 1

val Any?.staticProperty get() = 2

fun test(d: dynamic, staticParameter: Any?.() -> Unit) {
    d.staticExtension()
    d.staticProperty
    d.staticParameter
}
