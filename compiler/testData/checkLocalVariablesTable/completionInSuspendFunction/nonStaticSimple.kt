// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

class A {
    suspend fun foo() {}
}

// METHOD : A.foo(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LA; INDEX=0
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=1
