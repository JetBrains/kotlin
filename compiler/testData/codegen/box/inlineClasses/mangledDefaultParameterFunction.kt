// !LANGUAGE: +InlineClasses
inline class X(val s: String)
fun foo(x: X, block: (X) -> String = { it.s }) = block(x)

fun box(): String {
    return foo(X("OK"))
}
