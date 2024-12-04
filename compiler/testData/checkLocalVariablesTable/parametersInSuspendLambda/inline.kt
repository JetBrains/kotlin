// WITH_STDLIB
data class A(val x: String, val y: String)

suspend inline fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A("O", "K")) { (x_param, y_param) -> x_param + y_param }

// METHOD : InlineKt.test(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=0
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=0
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=6
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=7
// VARIABLE : NAME=$i$a$-foo-InlineKt$test$2 TYPE=I INDEX=5
// VARIABLE : NAME=a$iv TYPE=LA; INDEX=1
// VARIABLE : NAME=$i$f$foo TYPE=I INDEX=2
