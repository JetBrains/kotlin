// WITH_STDLIB
data class A(val x: String, val y: String)

suspend inline fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A("O", "K")) { (x_param, y_param) -> x_param + y_param }

// METHOD : InlineKt.test(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation;
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation;
// VARIABLE : NAME=$i$a$-foo-InlineKt$test$2 TYPE=I
// VARIABLE : NAME=$i$f$foo TYPE=I
// VARIABLE : NAME=a$iv TYPE=LA;
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String;
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String;
