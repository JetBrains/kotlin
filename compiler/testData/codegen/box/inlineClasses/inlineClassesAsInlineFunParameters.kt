// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z(val int: Int)
inline class L(val long: Long)
inline class Str(val string: String)
inline class Obj(val obj: Any)

inline fun <R> s1Z(x: Z, fn: (Int, Z) -> R) = fn(1, x)
inline fun <R> s1L(x: L, fn: (Int, L) -> R) = fn(1, x)
inline fun <R> s1Str(x: Str, fn: (Int, Str) -> R) = fn(1, x)
inline fun <R> s1Obj(x: Obj, fn: (Int, Obj) -> R) = fn(1, x)

fun testS1Z(a: Z) = s1Z(a) { i, xx -> Z(xx.int + i) }
fun testS1L(a: L) = s1L(a) { i, xx -> L(xx.long + i.toLong()) }
fun testS1Str(a: Str) = s1Str(a) { i, xx -> Str(xx.string + i.toString()) }
fun testS1Obj(a: Obj) = s1Obj(a) { i, xx -> Obj(xx.obj.toString() + i.toString()) }

fun box(): String {
    if (testS1Z(Z(42)).int != 43) throw AssertionError()
    if (testS1L(L(42L)).long != 43L) throw AssertionError()
    if (testS1Str(Str("abc")).string != "abc1") throw AssertionError()
    if (testS1Obj(Obj("abc")).obj != "abc1") throw AssertionError()

    return "OK"
}