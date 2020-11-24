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

// TODO: The backends disagree on the local variables in invoke/invokeSuspend methods


// LOCAL VARIABLES
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:4 <init>:
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// CoroutineUtil.kt:28 getContext:

// LOCAL VARIABLES JVM
// test.kt:-1 <init>:
// test.kt:-1 create: value:java.lang.Object=A, completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:-1 invoke:

// LOCAL VARIABLES JVM_IR
// test.kt:-1 <init>: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:-1 create: value:java.lang.Object=A, $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:-1 invoke: p1:A=A, p2:kotlin.coroutines.Continuation=helpers.ResultContinuation

// LOCAL VARIABLES
// test.kt:12 invokeSuspend:
// test.kt:5 component1:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, $dstr$x_param$_u24__u24$y_param:A=A
// test.kt:7 component3:

// LOCAL VARIABLES JVM
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, $dstr$x_param$_u24__u24$y_param:A=A

// LOCAL VARIABLES JVM_IR
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, $dstr$x_param$_u24__u24$y_param:A=A, x_param:java.lang.String="O":java.lang.String

// LOCAL VARIABLES
// test.kt:13 invokeSuspend: $result:java.lang.Object=kotlin.Unit, x_param:java.lang.String="O":java.lang.String, y_param:java.lang.String="K":java.lang.String

// LOCAL VARIABLES JVM
// test.kt:-1 invoke:

// LOCAL VARIABLES JVM_IR
// test.kt:-1 invoke: p1:A=A, p2:kotlin.coroutines.Continuation=helpers.ResultContinuation

// LOCAL VARIABLES
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation