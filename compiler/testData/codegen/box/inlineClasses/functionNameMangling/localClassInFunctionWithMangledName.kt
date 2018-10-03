// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class S(val string: String)

fun foo(s: S): String {
    class Local {
        fun bar() = s.string
    }
    return Local().bar()
}

fun box() = foo(S("OK"))