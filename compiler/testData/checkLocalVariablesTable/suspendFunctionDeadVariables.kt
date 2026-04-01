// WITH_STDLIB
// API_VERSION: LATEST

suspend fun dummy() {}

suspend fun test() {
    dummy()
    val a = 0
}

// METHOD : SuspendFunctionDeadVariablesKt.test(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation;
// VARIABLE : NAME=$continuation TYPE=Lkotlin/coroutines/Continuation;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=a TYPE=I
