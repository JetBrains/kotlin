// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Value(val value: Int)

object Foo {
    fun foo(value: Value) {
        res = value.value
    }

    fun bar(value: Value?) {
        res = value?.value
    }
}

var res: Int? = 0

fun box(): String {
    Value(42).let(Foo::foo)
    if (res != 42) return "FAIL 1 $res"
    res = 0

    Value(42).let(Foo::bar)
    if (res != 42) return "FAIL 2 $res"
    res = 0

    null.let(Foo::bar)
    if (res != null) return "FAIL 3: $res"

    return "OK"
}