// WITH_STDLIB

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

// EXPECTATIONS JVM_IR
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:5 component1:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit
// test.kt:7 component3:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, x_param:java.lang.String="O":java.lang.String
// test.kt:13 invokeSuspend: $result:java.lang.Object=kotlin.Unit, x_param:java.lang.String="O":java.lang.String, y_param:java.lang.String="K":java.lang.String
// test.kt:-1 invoke: p1:A=A, p2:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:12 box: $completion=EmptyContinuation
// test.kt:4 <init>:
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:12 box$slambda:
// test.kt:12 box: $completion=EmptyContinuation
// test.kt:10 foo: a=A, block=Function2, $completion=EmptyContinuation
// test.kt:12 doResume:
// test.kt:5 component1:
// test.kt:12 doResume: x_param="O":kotlin.String
// test.kt:7 component3:
// test.kt:13 doResume: x_param="O":kotlin.String, y_param="K":kotlin.String
