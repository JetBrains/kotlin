// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Z(val int: Int)
inline class L(val long: Long)
inline class Str(val string: String)
inline class Obj(val obj: Any)

inline class Host(val xx: Int) {
    inline fun <R> withDefaultZ(fn: (Z) -> R, x: Z = Z(xx)) = fn(x)
    inline fun <R> withDefaultL(fn: (L) -> R, x: L = L(xx.toLong())) = fn(x)
    inline fun <R> withDefaultL2(x: L = L(xx.toLong()), fn: (L) -> R) = fn(x)
    inline fun <R> withDefaultStr(fn: (Str) -> R, x: Str = Str(xx.toString())) = fn(x)
    inline fun <R> withDefaultObj(fn: (Obj) -> R, x: Obj = Obj(xx.toString())) = fn(x)
    inline fun <R> withDefaultObj2(x: Obj = Obj(xx.toString()), fn: (Obj) -> R) = fn(x)

    fun testWithDefaultZ() = withDefaultZ({ Z(it.int + 1) })
    fun testWithDefaultL() = withDefaultL({ L(it.long + 1L) })
    fun testWithDefaultL2() = withDefaultL2(fn = { L(it.long + 1L) })
    fun testWithDefaultStr() = withDefaultStr({ Str(it.string + "1") })
    fun testWithDefaultObj() = withDefaultObj({ Obj(it.obj.toString() + "1") })
    fun testWithDefaultObj2() = withDefaultObj2(fn = { Obj(it.obj.toString() + "1") })
}

fun box(): String {
    val h = Host(42)
    if (h.testWithDefaultZ().int != 43) throw AssertionError()
    if (h.testWithDefaultL().long != 43L) throw AssertionError()
    if (h.testWithDefaultL2().long != 43L) throw AssertionError()
    if (h.testWithDefaultStr().string != "421") throw AssertionError()
    if (h.testWithDefaultObj().obj != "421") throw AssertionError()
    if (h.testWithDefaultObj2().obj != "421") throw AssertionError()

    return "OK"
}