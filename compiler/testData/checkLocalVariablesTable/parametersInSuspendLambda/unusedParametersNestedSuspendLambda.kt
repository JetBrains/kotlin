// WITH_STDLIB
suspend fun foo(block: suspend (Int, String) -> String): String = block(1, "O")

suspend fun noSuspend() {}

var nested: (suspend () -> Unit)? = null

fun launch(block: suspend () -> Unit) {
    nested = block
}

suspend fun test() = foo { used, unused ->
    launch { noSuspend() }
    used.toString()
}

// METHOD : UnusedParametersNestedSuspendLambdaKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LUnusedParametersNestedSuspendLambdaKt$test$2;
// VARIABLE : NAME=unused TYPE=Ljava/lang/String;
// VARIABLE : NAME=used TYPE=I
