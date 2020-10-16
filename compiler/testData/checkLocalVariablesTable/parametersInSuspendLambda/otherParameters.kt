// WITH_RUNTIME
data class A(val x: String, val y: String)

suspend fun foo(a: A, block: suspend (Int, A, String) -> String): String = block(1, a, "#")

suspend fun test() = foo(A("O", "K")) { i_param, (x_param, y_param), v_param ->
    i_param.toString() + x_param + y_param + v_param
}

// Parameters (including anonymous destructuring parameters) are moved to fields in the Continuation class for the suspend lambda class.
// However, in non-IR, the fields are first stored in local variables, and they are not read directly (even for destructuring components).
// In IR, the fields are directly read from.

// METHOD : OtherParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

// JVM_TEMPLATES
// VARIABLE : NAME=i_param TYPE=I INDEX=2
// VARIABLE : NAME=$dstr$x_param$y_param TYPE=LA; INDEX=3
// VARIABLE : NAME=v_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=5
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=6
// VARIABLE : NAME=this TYPE=LOtherParametersKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=this TYPE=LOtherParametersKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1
