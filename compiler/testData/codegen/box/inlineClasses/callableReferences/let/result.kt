// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: WASM

object Foo {
    fun foo(result: Result<String>) {
        res = result.getOrNull()!!
    }

    fun bar(result: Result<String>?) {
        res = result?.getOrNull()!!
    }
}

var res = "FAIL"

fun box(): String {
    Result.success("OK").let(Foo::foo)
    if (res != "OK") return "FAIL 1 $res"
    res = "FAIL"

    Result.success("OK").let(Foo::bar)
    return res
}