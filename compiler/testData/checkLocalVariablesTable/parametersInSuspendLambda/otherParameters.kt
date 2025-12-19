// WITH_STDLIB
data class A(val x: String, val y: String)

suspend fun foo(a: A, block: suspend (Int, A, String) -> String): String = block(1, a, "#")

suspend fun test() = foo(A("O", "K")) { i_param, (x_param, y_param), v_param ->
    i_param.toString() + x_param + y_param + v_param
}

// METHOD : OtherParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=<destruct> TYPE=LA;
// VARIABLE : NAME=i_param TYPE=I
// VARIABLE : NAME=this TYPE=LOtherParametersKt$test$2;
// VARIABLE : NAME=v_param TYPE=Ljava/lang/String;
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String;
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String;
