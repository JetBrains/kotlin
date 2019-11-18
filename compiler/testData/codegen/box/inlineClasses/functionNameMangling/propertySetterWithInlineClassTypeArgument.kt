// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Str(val string: String)

class C {
    var s = Str("")
}

fun box(): String {
    val x = C()
    x.s = Str("OK")
    return x.s.string
}