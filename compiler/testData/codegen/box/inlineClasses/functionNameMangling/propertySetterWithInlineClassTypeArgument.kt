// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str(val string: String)

class C {
    var s = Str("")
}

fun box(): String {
    val x = C()
    x.s = Str("OK")
    return x.s.string
}