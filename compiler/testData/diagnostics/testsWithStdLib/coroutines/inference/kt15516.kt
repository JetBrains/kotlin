// !WITH_NEW_INFERENCE
// SKIP_TXT
class StateMachine<Q> internal constructor() {
    fun getInputStub(): Q = null <!UNCHECKED_CAST!>as Q<!>
}

fun <T> stateMachine(<!UNUSED_PARAMETER!>block<!>: suspend StateMachine<T>.() -> Unit): StateMachine<T> {
    return StateMachine<T>()
}

class Problem<F>(){
    fun getInputStub(): F = null <!UNCHECKED_CAST!>as F<!>

    fun createStateMachine(): StateMachine<F> = stateMachine {
        val letter = getInputStub()
        if (letter is Any)
            println("yes")
    }
}
