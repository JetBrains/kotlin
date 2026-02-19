var result1 = "failed"
var result2 = "failed"

var temp: Any = 1
var <T> T.foo: T.()-> String
    get() = temp as T.()-> String
    set(value) { temp = value }

val Int.bar: Int.(String)-> String
    get() = { y: String -> y }

fun box(): String {
    val a = Int::foo
    a.set(1, {"O"})
    result1 = a.get(1)(1)

    val b = Int::bar.get(1)
    result2 = b(1, "K")

    return result1 + result2
}
