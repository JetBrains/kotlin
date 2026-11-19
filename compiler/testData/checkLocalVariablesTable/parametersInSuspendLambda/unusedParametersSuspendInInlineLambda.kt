// WITH_STDLIB
suspend fun foo(block: suspend (Int, String) -> String): String = block(1, "O")

suspend fun noSuspend() {}

suspend fun test() = foo { used, unused ->
    run { noSuspend() }
    used.toString()
}

// METHOD : UnusedParametersSuspendInInlineLambdaKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$i$a$-run-UnusedParametersSuspendInInlineLambdaKt$test$2$1 TYPE=I
// VARIABLE : NAME=$i$a$-run-UnusedParametersSuspendInInlineLambdaKt$test$2$1 TYPE=I
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LUnusedParametersSuspendInInlineLambdaKt$test$2;
// VARIABLE : NAME=used TYPE=I
