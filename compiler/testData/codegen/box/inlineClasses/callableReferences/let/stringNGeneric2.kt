// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Value<T: String>(val value: T?)

object Foo {
    fun foo(value: Value<String>) {
        res = value.value
    }

    fun bar(value: Value<String>?) {
        res = value?.value
    }
}

var res: String? = "FAIL"

fun box(): String {
    Value<String>("OK").let(Foo::foo)
    if (res != "OK") return "FAIL 1: $res"
    res = "FAIL 2"

    Value<String>("OK").let(Foo::bar)
    if (res != "OK") return "FAIL 3: $res"
    res = "FAIL 4"

    null.let(Foo::bar)
    if (res != null) return "FAIL 3: $res"
    return "OK"
}