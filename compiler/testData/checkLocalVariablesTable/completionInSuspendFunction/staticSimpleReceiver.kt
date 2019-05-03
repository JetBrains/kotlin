// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

class A

suspend fun A.foo() {}

// METHOD : StaticSimpleReceiverKt.foo(LA;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=$this$foo TYPE=LA; INDEX=0
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=1