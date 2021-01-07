class A {
    suspend fun f() {}

    internal inline suspend fun g() {
        f()
        f()
    }
}

// 1 public final g\$test_module\(Lkotlin/coroutines/Continuation;\)Ljava/lang/Object;
// 1 private final g\$test_module\$\$forInline\(Lkotlin/coroutines/Continuation;\)Ljava/lang/Object;
// 0 g\$\$forInline