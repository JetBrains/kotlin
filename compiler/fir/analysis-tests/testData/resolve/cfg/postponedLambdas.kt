// !DUMP_CFG
inline fun foo(vararg x: Any) {}

fun test(a: Any, b: Any, c: Any) {
    foo(a, { "" }, b)
}