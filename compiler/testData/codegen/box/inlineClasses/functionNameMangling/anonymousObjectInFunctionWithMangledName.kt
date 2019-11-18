// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class S(val string: String)

fun foo(s: S): String {
    val anon = object {
        fun bar() = s.string
    }
    return anon.bar()
}

fun box() = foo(S("OK"))