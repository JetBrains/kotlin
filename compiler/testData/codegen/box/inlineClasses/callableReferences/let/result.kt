// WITH_STDLIB
// IGNORE_BACKEND: WASM

object Foo {
    fun foo(result: Result<String>) {
        res = result.getOrNull()
    }

    fun bar(result: Result<String>?) {
        res = result?.getOrNull()
    }
}

var res: String? = "FAIL"

fun box(): String {
    Result.success("OK").let(Foo::foo)
    if (res != "OK") return "FAIL 1 $res"
    res = "FAIL 2"

    Result.success("OK").let(Foo::bar)
    if (res != "OK") return "FAIL 3 $res"
    res = "FAIL 4"

    null.let(Foo::bar)
    if (res != null) return "FAIL 5: $res"
    return "OK"
}