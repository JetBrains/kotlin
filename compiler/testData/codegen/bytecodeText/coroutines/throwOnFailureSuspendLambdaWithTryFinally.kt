// WITH_COROUTINES
// TREAT_AS_ONE_FILE

suspend fun foo() {}
suspend fun bar() {}

fun builder(c: suspend () -> Unit) {}

fun test() {
    builder {
        try {
            foo()
        } finally {
            bar()
        }
    }
}

// When a suspension point is inside try-finally, hoisting is disabled (hasEnclosingTryCatch = true).
// Additionally, finally blocks with suspension points are duplicated for the exceptional handler path,
// creating 3 suspension points (foo, bar on normal path, bar on exceptional path) + state 0 = 4 invocations.
// 4 INVOKESTATIC kotlin/ResultKt.throwOnFailure \(Ljava/lang/Object;\)V
