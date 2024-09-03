var result = ""

class A

val A?.foo: String
    get() = "O"

val <T> T?.bar: String
    get() = "K"

fun box(): String {
    val a = A?::foo
    result += a(null)

    val b = Int?::bar
    result += b(null)
    return result
}