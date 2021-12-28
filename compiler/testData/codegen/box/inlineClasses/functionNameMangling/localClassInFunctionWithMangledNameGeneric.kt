// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

fun foo(s: S<String>): String {
    class Local {
        fun bar() = s.string
    }
    return Local().bar()
}

fun box() = foo(S("OK"))