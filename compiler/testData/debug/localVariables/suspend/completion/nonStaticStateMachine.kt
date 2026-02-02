// LANGUAGE: +JvmNullOutSpilledCoroutineLocals

// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^ This test is very flaky on JS due to a Node.js bug https://github.com/nodejs/node/issues/45410
// FILE: test.kt
class A {
    suspend fun foo() {}
    suspend fun foo1(l: Long) {
        foo()
        foo()
        val dead = l
    }
}

suspend fun box() {
    A().foo1(42)
}

// EXPECTATIONS JVM_IR
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 <init>:
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:8 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:9 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:7 foo: $completion:kotlin.coroutines.Continuation=A$foo1$1
// test.kt:9 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:10 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:7 foo: $completion:kotlin.coroutines.Continuation=A$foo1$1
// test.kt:10 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:11 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:12 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, dead:long=42:long
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS WASM
// test.kt:16 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (4, 4)
// test.kt:13 $A.<init>: $<this>:(ref $A)=(ref $A) (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:16 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (13, 13, 8)
// test.kt:12 $A.foo1: $<this>:(ref $A)=(ref $A), $l:i64=i64, $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (5, 5, 5, 5, 5)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:12 $A.foo1: $<this>:(ref $A)=(ref $A), $l:i64=i64, $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (5, 5, 5, 5, 5, 5, 5, 5)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:7 $A.foo: $<this>:(ref $A)=(ref $A), $$completion:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$) (23, 23)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8, 8, 8, 8)
// test.kt:12 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (5, 5)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:10 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8)
// test.kt:7 $A.foo: $<this>:(ref $A)=(ref $A), $$completion:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$) (23, 23)
// test.kt:10 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8, 8, 8, 8)
// test.kt:12 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (5, 5)
// test.kt:9 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:10 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8, 8, 8, 8, 8, 8)
// test.kt:11 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (19, 8, 8)
// test.kt:12 $$foo1COROUTINE$.doResume: $<this>:(ref $$foo1COROUTINE$)=(ref $$foo1COROUTINE$), $dead:i64=i64, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4)
// test.kt:12 $A.foo1: $<this>:(ref $A)=(ref $A), $l:i64=i64, $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (5)
// test.kt:16 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (8, 8)
