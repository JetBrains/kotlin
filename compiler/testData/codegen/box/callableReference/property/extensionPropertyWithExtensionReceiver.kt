var result1 = "failed"
var result2 = "failed"

val (Int.()-> String).foo: String
    get() = this(1)

val <T> (Int.()-> T).bar: T
    get() = this(1)

fun box(): String {
    fun Int.test1():String { return "O" }
    val a = Int::test1::foo
    result1 = a()

    fun Int.test2():String { return "K" }
    val b = Int::test2::bar
    result2 = b()

    return result1 + result2
}