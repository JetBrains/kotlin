class Controller {
    suspend fun suspendHere() = ""
}


fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {

    for (i in 1..3) {
        builder {
            if (suspendHere() != "OK") throw java.lang.RuntimeException("fail 1")
        }
    }

    return "OK"
}

// 1 GETSTATIC kotlin/Unit.INSTANCE : Lkotlin/Unit;
// 1 GETSTATIC kotlin/coroutines/Suspend.INSTANCE
// 2 GETSTATIC
