// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
interface A {
    fun foo()
}

fun takeA(a: A): Boolean = true

fun test(a: Any) {
    if (takeA(a as? A ?: return)) {
        a.foo()
    }
}
