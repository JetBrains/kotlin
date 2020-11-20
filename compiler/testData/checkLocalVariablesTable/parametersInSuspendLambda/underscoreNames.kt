// WITH_RUNTIME
class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

suspend fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun test() = foo(A()) { (x_param, _, y_param) ->
    x_param + y_param
}

// Parameters (including anonymous destructuring parameters) are moved to fields in the Continuation class for the suspend lambda class.
// However, in non-IR, the fields are first stored in local variables, and they are not read directly (even for destructuring components).
// In IR, the fields are directly read from.

// METHOD : UnderscoreNamesKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

// JVM_TEMPLATES
// VARIABLE : NAME=$dstr$x_param$_u24__u24$y_param TYPE=LA; INDEX=2
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1
