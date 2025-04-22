// IGNORE_NATIVE: compatibilityTestMode=FORWARD
// ^^^ This new test fails under 2.1.0 compiler with IndexOutOfBoundsException and passes on 2.2.0 and later
var result = ""

fun interface SamInterface {
    fun Int.accept(i: String): String
}

fun Int.foo(i: String): String = i
fun bar(a: Int, i: String): String = i

val a = SamInterface(Int::foo)
val b = SamInterface(::bar)

fun box(): String {
    with(a) {
        result += 1.accept("O")
    }
    with(b) {
        result += 1.accept("K")
    }
    return result
}