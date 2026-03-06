// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class NumberInlineClass(val value: Double)

interface TypeAdapter<FROM, TO> {
    fun decode(string: FROM): TO
}

class StringToDoubleTypeAdapter : TypeAdapter<String, NumberInlineClass> {
    override fun decode(string: String) = NumberInlineClass(string.toDouble())
}

fun box(): String {
    val string: String? = "2019"
    val typeAdapter = StringToDoubleTypeAdapter()
    val test = string?.let(typeAdapter::decode)!!
    if (test.value != 2019.0) throw AssertionError("test: $test")
    return "OK"
}