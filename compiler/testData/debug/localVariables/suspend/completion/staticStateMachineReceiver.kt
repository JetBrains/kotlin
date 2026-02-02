// LANGUAGE: +JvmNullOutSpilledCoroutineLocals

// FILE: test.kt
class A

suspend fun A.foo() {}
suspend fun A.foo1(l: Long) {
    foo()
    foo()
    val dead = l
}

suspend fun box() {
    A().foo1(42)
}

// EXPECTATIONS JVM_IR
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:7 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:8 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:8 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:9 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:9 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:10 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:11 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, dead:long=42:long
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:15 box: $completion=EmptyContinuation
// test.kt:4 <init>:
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:8 doResume:
// test.kt:6 foo: <this>=A, $completion=Coroutine
// test.kt:9 doResume:
// test.kt:6 foo: <this>=A, $completion=Coroutine
// test.kt:10 doResume:
// test.kt:11 doResume: dead=kotlin.Long

// EXPECTATIONS WASM
// test.kt:14 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (4, 4)
// test.kt:4 $A.<init>: $<this>:(ref $A)=(ref $A) (7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7)
// test.kt:14 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (13, 13, 8)
// test.kt:11 $foo1: $<this>:(ref $A)=(ref $A), $l:i64=i64, $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:11 $foo1: $<this>:(ref $A)=(ref $A), $l:i64=i64, $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:8 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:6 $foo: $<this>:(ref $A)=(ref $A), $$completion:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$) (21, 21)
// test.kt:8 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4)
// test.kt:11 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:8 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4)
// test.kt:6 $foo: $<this>:(ref $A)=(ref $A), $$completion:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$) (21, 21)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4)
// test.kt:11 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:8 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4)
// test.kt:10 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (15, 4, 4)
// test.kt:11 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (0)
// test.kt:11 $foo1: $<this>:(ref $A)=(ref $A), $l:i64=i64, $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1)
// test.kt:14 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (8, 8)
