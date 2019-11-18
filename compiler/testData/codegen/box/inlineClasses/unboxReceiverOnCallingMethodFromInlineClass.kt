// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Foo(val s: Any) {
    fun isString(): Boolean = s is String
}

class Box<T>(val x: T)

fun box(): String {
    val f = Foo("string")
    val g = Box(f)
    val r = g.x.isString()

    if (!r) return "Fail"

    return "OK"
}