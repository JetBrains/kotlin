// WITH_RUNTIME

suspend fun dummy() {}

suspend fun test() {
    dummy()
    val a = 0
}

// METHOD : SuspendFunctionDeadVariablesKt.test(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

// VARIABLE : NAME=a TYPE=I INDEX=1
// VARIABLE : NAME=$continuation TYPE=Lkotlin/coroutines/Continuation; INDEX=3
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=2