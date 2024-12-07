// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

@JvmInline
value class Str<T: String>(val s: T)

@JvmOverloads
fun test(so: String = "O", sk: String = "K") = Str(so + sk)

fun box(): String =
    test().s