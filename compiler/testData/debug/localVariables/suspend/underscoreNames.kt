// WITH_RUNTIME
// WITH_COROUTINES
// FILE: test.kt
class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

suspend fun foo(a: A, block: suspend (A) -> String): String = block(a)

suspend fun box() = foo(A()) { (x_param, _, y_param) ->
    x_param + y_param
}

// Parameters (including anonymous destructuring parameters) are moved to fields in the Continuation class for the suspend lambda class.
// However, in non-IR, the fields are first stored in local variables, and they are not read directly (even for destructuring components).
// In IR, the fields are directly read from.

// METHOD : UnderscoreNamesKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

// JVM_TEMPLATES
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=y_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:4 <init>:
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// CoroutineUtil.kt:28 getContext:
// test.kt:-1 <init>:
// test.kt:-1 create: value:java.lang.Object=A, completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:-1 invoke:
// test.kt:12 invokeSuspend:
// test.kt:5 component1:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, $dstr$x_param$_u24__u24$y_param:A=A
// test.kt:7 component3:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, $dstr$x_param$_u24__u24$y_param:A=A
// test.kt:13 invokeSuspend: $result:java.lang.Object=kotlin.Unit, x_param:java.lang.String="O":java.lang.String, y_param:java.lang.String="K":java.lang.String
// test.kt:-1 invoke:
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation