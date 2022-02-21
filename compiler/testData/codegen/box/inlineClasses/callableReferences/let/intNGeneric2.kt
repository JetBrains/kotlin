// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Value<T: Int>(val value: T?)

object Foo {
    fun foo(value: Value<Int>) {
        res = value.value
    }

    fun bar(value: Value<Int>?) {
        res = value?.value
    }
}

var res: Int? = 0

fun box(): String {
    Value<Int>(42).let(Foo::foo)
    if (res != 42) return "FAIL 1 $res"
    res = 0

    Value<Int>(42).let(Foo::bar)
    if (res != 42) return "FAIL 2 $res"
    res = 0

    null.let(Foo::bar)
    if (res != null) return "FAIL 3: $res"

    return "OK"
}