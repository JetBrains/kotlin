// !LANGUAGE: +NewInference
// WITH_RUNTIME

fun box(): String {
//    return "${getO()}K"
//    return "${getO()}${getK()}"
    return "O${getK()}"
}

/*fun getO(): String {
    val arguments = Tuple<Any?>(3)
    arguments[0] = "O"
    arguments[1] = 42
    return varargLambda(*arguments) { (arg1, arg2) -> "$arg1" } // Codegen failure
}*/
class SimpleObj(val value: Int) {
    operator fun minus(other: Int) = value - other
}

class OtherObj(val value: String) {
    fun foo() = value
}

fun getK() = varargLambda(SimpleObj(42), OtherObj("K"), 14) { arg1, arg2, plainInt ->
    val someResult = arg1 - plainInt
    assert(someResult == 28) { "Not 28" }
    "${arg2.foo()}"
}

fun <vararg Ts, R> varargLambda(
    vararg arguments: *Ts,
    block: (*Ts) -> R
): R {
    return block(arguments)
}