// WITH_STDLIB
// LANGUAGE: +JvmNullOutSpilledCoroutineLocals
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

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit
// test.kt:5 component1:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, <name for destructuring parameter 0>:A=A
// test.kt:7 component3:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, <name for destructuring parameter 0>:A=A, x_param:java.lang.String="O":java.lang.String
// test.kt:13 invokeSuspend: $result:java.lang.Object=kotlin.Unit, <name for destructuring parameter 0>:A=A, x_param:java.lang.String="O":java.lang.String, y_param:java.lang.String="K":java.lang.String
// test.kt:-1 invoke: p1:A=A, p2:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS FIR JVM_IR
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 foo: a:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$2, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit
// test.kt:5 component1:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, <destruct>:A=A
// test.kt:7 component3:
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit, <destruct>:A=A, x_param:java.lang.String="O":java.lang.String
// test.kt:13 invokeSuspend: $result:java.lang.Object=kotlin.Unit, <destruct>:A=A, x_param:java.lang.String="O":java.lang.String, y_param:java.lang.String="K":java.lang.String
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

// EXPECTATIONS WASM
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:14 $$box__JsExportAdapterCOROUTINE$.doResume: $<this>:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:12 $box: $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$) (24, 24)
// test.kt:8 $A.<init>: $<this>:(ref $A)=(ref $A) (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:12 $box: $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$) (24, 24, 20)
// test.kt:10 $foo: $a:(ref $A)=(ref $A), $block:(ref $box$slambda)=(ref $box$slambda), $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$) (62, 68, 68, 62, 62, 62, 62, 62, 62)
// test.kt:12 $box$slambda.invoke: $<this>:(ref $box$slambda)=(ref $box$slambda), $<destruct>:(ref $A)=(ref $A), $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$), $x_param:(ref null $kotlin.String)=null, $y_param:(ref null $kotlin.String)=null (32, 32)
// test.kt:5 $A.component1: $<this>:(ref $A)=(ref $A) (32, 32, 32, 35)
// test.kt:12 $box$slambda.invoke: $<this>:(ref $box$slambda)=(ref $box$slambda), $<destruct>:(ref $A)=(ref $A), $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$), $x_param:(ref $kotlin.String)=(ref $kotlin.String), $y_param:(ref null $kotlin.String)=null (32, 44, 44)
// test.kt:7 $A.component3: $<this>:(ref $A)=(ref $A) (32, 32, 32, 35)
// test.kt:12 $box$slambda.invoke: $<this>:(ref $box$slambda)=(ref $box$slambda), $<destruct>:(ref $A)=(ref $A), $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$), $x_param:(ref $kotlin.String)=(ref $kotlin.String), $y_param:(ref null $kotlin.String)=null (44)
// test.kt:13 $box$slambda.invoke: $<this>:(ref $box$slambda)=(ref $box$slambda), $<destruct>:(ref $A)=(ref $A), $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$), $x_param:(ref $kotlin.String)=(ref $kotlin.String), $y_param:(ref $kotlin.String)=(ref $kotlin.String) (4, 14, 4, 21)
// test.kt:10 $foo: $a:(ref $A)=(ref $A), $block:(ref $box$slambda)=(ref $box$slambda), $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$) (70, 70)
// test.kt:14 $box: $$completion:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$) (1, 1)
// test.kt:14 $$box__JsExportAdapterCOROUTINE$.doResume: $<this>:(ref $$box__JsExportAdapterCOROUTINE$)=(ref $$box__JsExportAdapterCOROUTINE$), $suspendResult:(ref $kotlin.String)=(ref $kotlin.String), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
