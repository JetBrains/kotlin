// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str(val s: String)

@JvmOverloads
fun test(so: String = "O", sk: String = "K") = Str(so + sk)

fun box(): String =
    test().s