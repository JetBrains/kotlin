// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

suspend fun foo() {}
suspend fun foo1(l: Long) {
    foo()
    foo()
}

// METHOD : StaticStateMachineKt.foo1(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=l TYPE=J INDEX=0
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=2
// VARIABLE : NAME=$continuation TYPE=Lkotlin/coroutines/Continuation; INDEX=4