// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

fun foo(s: S): String {
    class Local {
        fun bar() = s.string
    }
    return Local().bar()
}

fun box() = foo(S("OK"))