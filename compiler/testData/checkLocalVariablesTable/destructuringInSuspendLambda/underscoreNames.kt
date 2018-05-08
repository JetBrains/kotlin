class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

suspend fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A()) { (x_param, _, y_param) -> x_param + y_param }

// METHOD : UnderscoreNamesKt$test$2.doResume(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$test$2; INDEX=0
// VARIABLE : NAME=data TYPE=Ljava/lang/Object; INDEX=1
// VARIABLE : NAME=throwable TYPE=Ljava/lang/Throwable; INDEX=2
// VARIABLE : NAME=$x_param_$_$_y_param TYPE=LA; INDEX=3
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=5
