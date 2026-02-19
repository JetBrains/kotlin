var result1 = "failed"
var result2 = "failed"

var Int.foo: String
    get() = "O"
    set(value: String) {}

var <T> T.bar: T
    get() = "K" as T
    set(value: T) {}

fun box(): String {
    val a = Int::foo
    result1 = a.get(1)

    val b = String::bar.get("")
    result2 = b

    return result1 + result2
}