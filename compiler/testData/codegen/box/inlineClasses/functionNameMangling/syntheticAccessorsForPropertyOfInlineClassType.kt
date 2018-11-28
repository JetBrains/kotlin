// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class S(val string: String)

class Outer {
    private var pr = S("")

    inner class Inner() {
        fun updateOuter(string: String): String {
            pr = S(string)
            return pr.string
        }
    }
}

fun box(): String =
    Outer().Inner().updateOuter("OK")