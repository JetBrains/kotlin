package test
class Controller {
    suspend fun suspendFun(x: Continuation<String>) {}
    operator fun handleResult(x: Int, y: Continuation<Nothing>) {}
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {

}
