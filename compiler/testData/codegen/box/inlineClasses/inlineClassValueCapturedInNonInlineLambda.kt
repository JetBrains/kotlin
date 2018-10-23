// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z(val int: Int)
inline class L(val long: Long)
inline class Str(val string: String)
inline class Obj(val obj: Any)

fun box(): String {
    var xz = Z(0)
    var xl = L(0L)
    var xs = Str("")
    var xo = Obj("")

    val fn = {
        xz = Z(42)
        xl = L(1234L)
        xs = Str("abc")
        xo = Obj("def")
    }
    fn()

    if (xz.int != 42) throw AssertionError()
    if (xl.long != 1234L) throw AssertionError()
    if (xs.string != "abc") throw AssertionError()
    if (xo.obj != "def") throw AssertionError()

    return "OK"
}