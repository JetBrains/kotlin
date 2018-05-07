data class A<T, F>(val x: T, val y: F)

suspend fun <X, Y> foo(a: A<X, Y>, block: suspend (A<X, Y>) -> String) = block(a)

suspend fun test() = foo(A("OK", 1)) { (x_param, y_param) -> x_param + (y_param.toString()) }

// METHOD : GenericKt$test$2.doResume(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LGenericKt$test$2; INDEX=0
// VARIABLE : NAME=data TYPE=Ljava/lang/Object; INDEX=1
// VARIABLE : NAME=throwable TYPE=Ljava/lang/Throwable; INDEX=2
// VARIABLE : NAME=$x_param_y_param TYPE=LA; INDEX=3
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=y_param TYPE=I INDEX=5
