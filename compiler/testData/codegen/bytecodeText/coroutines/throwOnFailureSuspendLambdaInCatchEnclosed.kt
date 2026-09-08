// WITH_COROUTINES
// TREAT_AS_ONE_FILE

suspend fun foo() {}
suspend fun bar() {}

fun dummy() {}

fun builder(c: suspend () -> Unit) {}

fun test() {
    builder {
        try {
            try {
                dummy()
            } catch (e: Exception) {
                foo()
            }
        } catch (e: Exception) {
        }
        bar()
    }
}

// 3 INVOKESTATIC kotlin/ResultKt.throwOnFailure \(Ljava/lang/Object;\)V
