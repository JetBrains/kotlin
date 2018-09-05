// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

suspend fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A()) { (x_param, _, y_param) -> x_param + y_param }

// METHOD : UnderscoreNamesKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$test$2; INDEX=0
// VARIABLE : NAME=result TYPE=Ljava/lang/Object; INDEX=1
// VARIABLE : NAME=$x_param_$_$_y_param TYPE=LA; INDEX=2
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=4
