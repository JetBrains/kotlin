// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

class Test(val x: S<String>, val y: S<String> = S("K")) {
    val test = x.string + y.string
}

fun box() = Test(S("O")).test