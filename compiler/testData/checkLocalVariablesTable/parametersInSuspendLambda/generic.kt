// WITH_RUNTIME

data class A<T, F>(val x: T, val y: F)

suspend fun <X, Y> foo(a: A<X, Y>, block: suspend (A<X, Y>) -> String) = block(a)

suspend fun test() = foo(A("OK", 1)) { (x_param, y_param) ->
    x_param + (y_param.toString())
}

// METHOD : GenericKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

// JVM_TEMPLATES
// VARIABLE : NAME=$dstr$x_param$y_param TYPE=LA; INDEX=2
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=I INDEX=4
// VARIABLE : NAME=this TYPE=LGenericKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=I INDEX=4
// VARIABLE : NAME=this TYPE=LGenericKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1
