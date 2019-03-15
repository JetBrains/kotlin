// IGNORE_BACKEND: JVM_IR
// WITH_COROUTINES
// TREAT_AS_ONE_FILE

suspend fun foo() {}

suspend fun bar(): Int {
    foo()
    return 42
}

// 2 INVOKESTATIC kotlin/ResultKt.throwOnFailure \(Ljava/lang/Object;\)V
