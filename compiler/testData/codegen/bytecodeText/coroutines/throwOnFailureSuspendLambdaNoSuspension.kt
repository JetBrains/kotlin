// WITH_COROUTINES
// TREAT_AS_ONE_FILE

fun builder(c: suspend () -> Unit) {}

fun test() {
    builder {
        val x = 1
    }
}

// 1 INVOKESTATIC kotlin/ResultKt.throwOnFailure \(Ljava/lang/Object;\)V
