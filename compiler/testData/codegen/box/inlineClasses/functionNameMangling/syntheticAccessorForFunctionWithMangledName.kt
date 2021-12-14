// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

class Outer {
    private fun foo(s: S) = s.string

    inner class Inner(val string: String) {
        fun bar() = foo(S(string))
    }
}

fun box(): String = Outer().Inner("OK").bar()