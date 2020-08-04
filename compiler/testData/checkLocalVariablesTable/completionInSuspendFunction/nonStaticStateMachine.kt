// WITH_RUNTIME

class A {
    suspend fun foo() {}
    suspend fun foo1(l: Long) {
        foo()
        foo()
        val dead = l
    }
}

// METHOD : A.foo1(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LA; INDEX=0
// VARIABLE : NAME=l TYPE=J INDEX=1
// VARIABLE : NAME=$continuation TYPE=Lkotlin/coroutines/Continuation; INDEX=7
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=6