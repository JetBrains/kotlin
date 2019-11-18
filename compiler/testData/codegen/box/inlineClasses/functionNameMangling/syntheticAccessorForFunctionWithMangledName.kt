// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class S(val string: String)

class Outer {
    private fun foo(s: S) = s.string

    inner class Inner(val string: String) {
        fun bar() = foo(S(string))
    }
}

fun box(): String = Outer().Inner("OK").bar()