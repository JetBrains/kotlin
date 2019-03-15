// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline class Z(val int: Int)

inline class Str(val string: String)

inline class NStr(val string: String?)

fun fooZ(x: Z) = x

fun fooStr(x: Str) = x

fun fooNStr(x: NStr) = x


fun box(): String {
    val fnZ = ::fooZ
    if (fnZ.invoke(Z(42)).int != 42) throw AssertionError()

    val fnStr = ::fooStr
    if (fnStr.invoke(Str("str")).string != "str") throw AssertionError()

    val fnNStr = ::fooNStr
    if (fnNStr.invoke(NStr(null)).string != null) throw AssertionError()
    if (fnNStr.invoke(NStr("nstr")).string != "nstr") throw AssertionError()

    return "OK"
}