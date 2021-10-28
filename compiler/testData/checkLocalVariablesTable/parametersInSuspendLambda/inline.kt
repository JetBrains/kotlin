// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
data class A(val x: String, val y: String)

suspend inline fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A("O", "K")) { (x_param, y_param) -> x_param + y_param }

// TODO: Fix this after other issues in inlining suspend lambdas are resolved.

// METHOD : InlineKt.test(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;

// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=6
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=7

// JVM_TEMPLATES
// VARIABLE : NAME=$dstr$x_param$y_param TYPE=LA; INDEX=4
// VARIABLE : NAME=continuation TYPE=Lkotlin/coroutines/Continuation; INDEX=3
// VARIABLE : NAME=$i$a$-foo-InlineKt$test$2 TYPE=I INDEX=5
// VARIABLE : NAME=a$iv TYPE=LA; INDEX=1
// VARIABLE : NAME=$i$f$foo TYPE=I INDEX=2
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=0

// JVM_IR_TEMPLATES
// VARIABLE : NAME=continuation TYPE=Lkotlin/coroutines/Continuation; INDEX=3
// VARIABLE : NAME=$i$a$-foo-InlineKt$test$2 TYPE=I INDEX=5
// VARIABLE : NAME=a$iv TYPE=LA; INDEX=1
// VARIABLE : NAME=$i$f$foo TYPE=I INDEX=2
// VARIABLE : NAME=$completion TYPE=Lkotlin/coroutines/Continuation; INDEX=0
