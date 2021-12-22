// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Value<T: Any>(val value: T)

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
    Value("OK").let(Foo::foo)
    if (res != "OK") return "FAIL 1: $res"
    res = "FAIL 2"

    Value("OK").let(Foo::bar)
    if (res != "OK") return "FAIL 3: $res"
    res = "FAIL 4"

    null.let(Foo::bar)
    if (res != null) return "FAIL 5: $res"

    return "OK"
}