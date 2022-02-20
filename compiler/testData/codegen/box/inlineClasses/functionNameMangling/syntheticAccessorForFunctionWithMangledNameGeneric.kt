// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

class Outer {
    private fun foo(s: S<String>) = s.string

    inner class Inner(val string: String) {
        fun bar() = foo(S(string))
    }
}

fun box(): String = Outer().Inner("OK").bar()