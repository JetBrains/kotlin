// WITH_RUNTIME

suspend fun foo() {}
suspend fun foo1(l: Long) {
    foo()
    foo()
    val dead = l
}

// METHOD : StaticStateMachineKt.foo1(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=l TYPE=J INDEX=0
// VARIABLE : NAME=$continuation TYPE=Lkotlin/coroutines/Continuation; INDEX=6
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=5