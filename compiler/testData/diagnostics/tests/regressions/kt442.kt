// FIR_IDENTICAL
// KT-442 Type inference fails on with()

fun <T> funny(f : () -> T) : T = f()

fun testFunny() {
    val a : Int = funny {1}
}

fun <T> funny2(f : (t : T) -> T) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun testFunny2() {
    val a : Int = funny2 {it}
}

fun box() : String {
    return generic_invoker { it }
}

fun <T> generic_invoker(gen :  (String) -> T) : T {
    return gen("")
}

infix fun <T> T.with(f :  T.() -> Unit) {
    f()
}

fun main() {
    val a = 1 with {
        plus(1)
    }
}
