// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

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