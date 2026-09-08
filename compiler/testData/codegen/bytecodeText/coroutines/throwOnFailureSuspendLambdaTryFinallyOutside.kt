// WITH_COROUTINES
// TREAT_AS_ONE_FILE

suspend fun foo() {}
suspend fun bar() {}

fun dummy() {}

fun builder(c: suspend () -> Unit) {}

fun test() {
    builder {
        try {
            dummy()
        } finally {
            dummy()
        }
        foo()
        bar()
    }
}

// 1 INVOKESTATIC kotlin/ResultKt.throwOnFailure \(Ljava/lang/Object;\)V
