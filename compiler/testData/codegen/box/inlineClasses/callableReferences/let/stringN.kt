inline class Value(val value: String?)

object Foo {
    fun foo(value: Value) {
        res = value.value!!
    }

    fun bar(value: Value?) {
        res = value?.value!!
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