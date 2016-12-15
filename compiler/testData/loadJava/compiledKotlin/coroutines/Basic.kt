package test
class Controller {
    suspend fun suspendFun() {}
}

fun builder(c: suspend Controller.() -> Unit) {

}
