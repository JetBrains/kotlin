// ISSUE: KT-65812
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun Controller<*>.foo() {}
fun <V, Y> V.bar(y: Y) {}


fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
        // This call contributes an inference error to the shared CS
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>MyUnresolvedClass<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>(this)

        // A lot of calls using shared CS
        // Previoisly, for each of them we copied all the previous errors and adding them to the shared CS
        // Thus, having an exponential number of errors
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
    }
}
