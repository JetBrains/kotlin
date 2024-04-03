// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

class Z<T>(val s: T)

class Data(val x: Int)

inline fun <T> foo(z: Z<T>) = z.s

fun box(): String {
    try {
        val z = Z("zzz")
        val s = foo(z as Z<Data>)
        return "FAIL: $s"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
