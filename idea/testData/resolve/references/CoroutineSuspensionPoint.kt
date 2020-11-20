class Controller {
    suspend fun suspendHere(x: Continuation<Unit>) {
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {

}

fun foo() {
    builder {
        <caret>suspendHere()
    }
}

// REF: (in Controller).suspendHere(Continuation<Unit>)
