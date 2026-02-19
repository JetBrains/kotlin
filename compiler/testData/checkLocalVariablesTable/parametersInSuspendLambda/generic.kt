// WITH_STDLIB

data class A<T, F>(val x: T, val y: F)

suspend fun <X, Y> foo(a: A<X, Y>, block: suspend (A<X, Y>) -> String) = block(a)

suspend fun test() = foo(A("OK", 1)) { (x_param, y_param) ->
    x_param + (y_param.toString())
}

// METHOD : GenericKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=<destruct> TYPE=LA;
// VARIABLE : NAME=this TYPE=LGenericKt$test$2;
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String;
// VARIABLE : NAME=y_param TYPE=I
