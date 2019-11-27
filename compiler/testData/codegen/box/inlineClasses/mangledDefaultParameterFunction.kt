// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
inline class X(val s: String)
fun foo(x: X, block: (X) -> String = { it.s }) = block(x)

fun box(): String {
    return foo(X("OK"))
}
