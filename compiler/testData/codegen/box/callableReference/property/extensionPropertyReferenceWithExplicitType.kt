var result = "failed"

val Int.foo: String
    get() = "not"

val <T> T.bar: T
    get() = "f" as T

val (Int.()-> String).baz: String
    get() = "a"

val <T> (Int.()-> T).qux: T
    get() = "i" as T

val <T> T.quux: T.()-> String
    get() = { "l" }

val Int.corge: Int.(String)-> String
    get() = fun Int.(a: String): String { return "ed" }

fun box(): String {
    val a: Int.() -> String = Int::foo
    result = a(1)

    val b : String.() -> String = String::bar
    result += b("")

    fun Int.test1():String { return "" }
    val c : () -> String = Int::test1::baz
    result += c()

    fun Int.test2():String { return "" }
    val d: () -> String = Int::test2::qux
    result += d()

    val e: (Int) -> (Int.()-> String) = Int::quux
    result += e(1)(1)

    val f: (Int) -> (Int.(String) -> String) = Int::corge
    result += f(1)(1, "a")

    return if (result == "notfailed") "OK"
    else "fail"
}