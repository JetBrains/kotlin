// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class NumberInlineClass(val value: Double): IC()

interface TypeAdapter<FROM, TO> {
    fun decode(string: FROM): TO
}

class StringToDoubleTypeAdapter : TypeAdapter<String, IC> {
    override fun decode(string: String): IC = NumberInlineClass(string.toDouble())
}

fun box(): String {
    val typeAdapter = StringToDoubleTypeAdapter()
    val test = typeAdapter.decode("2019")
    if ((test as NumberInlineClass).value != 2019.0) throw AssertionError("test: $test")
    return "OK"
}