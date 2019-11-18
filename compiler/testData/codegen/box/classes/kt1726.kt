// IGNORE_BACKEND_FIR: JVM_IR
class Foo(
        var state : Int,
        val f : (Int) -> Int){

    fun next() : Int {
        val nextState = f(state)
        state = nextState
        return state
    }
}

fun box(): String {
    val f = Foo(23, {x -> 2 * x})
    return if (f.next() == 46) "OK" else "fail"
}
