// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z(val int: Int)
inline class L(val long: Long)
inline class Str(val string: String)
inline class Obj(val obj: Any)

inline fun <R> withDefaultZ(fn: (Z) -> R, x: Z = Z(42)) = fn(x)
inline fun <R> withDefaultL(fn: (L) -> R, x: L = L(42L)) = fn(x)
inline fun <R> withDefaultL2(x: L = L(42L), fn: (L) -> R) = fn(x)
inline fun <R> withDefaultStr(fn: (Str) -> R, x: Str = Str("abc")) = fn(x)
inline fun <R> withDefaultObj(fn: (Obj) -> R, x: Obj = Obj("abc")) = fn(x)
inline fun <R> withDefaultObj2(x: Obj = Obj("abc"), fn: (Obj) -> R) = fn(x)

fun testWithDefaultZ() = withDefaultZ({ Z(it.int + 1) })
fun testWithDefaultL() = withDefaultL({ L(it.long + 1L) })
fun testWithDefaultL2() = withDefaultL2(fn = { L(it.long + 1L) })
fun testWithDefaultStr() = withDefaultStr({ Str(it.string + "1") })
fun testWithDefaultObj() = withDefaultObj({ Obj(it.obj.toString() + "1") })
fun testWithDefaultObj2() = withDefaultObj2(fn = { Obj(it.obj.toString() + "1") })

fun box(): String {
    if (testWithDefaultZ().int != 43) throw AssertionError()
    if (testWithDefaultL().long != 43L) throw AssertionError()
    if (testWithDefaultL2().long != 43L) throw AssertionError()
    if (testWithDefaultStr().string != "abc1") throw AssertionError()
    if (testWithDefaultObj().obj != "abc1") throw AssertionError()
    if (testWithDefaultObj2().obj != "abc1") throw AssertionError()

    return "OK"
}