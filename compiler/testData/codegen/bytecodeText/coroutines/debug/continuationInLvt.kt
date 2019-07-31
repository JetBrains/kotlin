suspend fun dummy() {}

suspend fun tailCall() {
    dummy()
}

suspend fun stateMachine() {
    dummy()
    dummy()
}

// for tail-calls there is no need to add continuation to LVT
// 1 LOCALVARIABLE \$continuation Lkotlin/coroutines/Continuation; L.* 2
