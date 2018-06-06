data class A(val x: String, val y: String)

suspend fun foo(a: A, block: suspend (Int, A, String) -> String): String = block(1, a, "#")

suspend fun test() = foo(A("O", "K")) { i_param, (x_param, y_param), v_param -> i_param.toString() + x_param + y_param + v_param }

// METHOD : OtherParametersKt$test$2.doResume(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LOtherParametersKt$test$2; INDEX=0
// VARIABLE : NAME=data TYPE=Ljava/lang/Object; INDEX=1
// VARIABLE : NAME=throwable TYPE=Ljava/lang/Throwable; INDEX=2
// VARIABLE : NAME=$x_param_y_param TYPE=LA; INDEX=3
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=5
