inline class Value(val value: Any?)

object Foo {
    fun foo(value: Value) {
        res = value.value as String
    }

    fun bar(value: Value?) {
        res = value?.value as String
    }
}

var res = "FAIL"

fun box(): String {
    Value("OK").let(Foo::foo)
    if (res != "OK") return "FAIL 1: $res"
    res = "FAIL"

    Value("OK").let(Foo::bar)
    return res
}