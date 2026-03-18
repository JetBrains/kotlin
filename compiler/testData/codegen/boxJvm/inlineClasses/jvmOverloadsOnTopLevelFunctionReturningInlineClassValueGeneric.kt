// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +GenericInlineClassParameter

@JvmInline
value class Str<T: String>(val s: T)

@JvmOverloads
fun test(so: String = "O", sk: String = "K") = Str(so + sk)

fun box(): String =
    test().s