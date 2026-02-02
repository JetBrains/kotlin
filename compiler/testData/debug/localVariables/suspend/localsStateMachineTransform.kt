

// WITH_STDLIB

// FILE: test.kt
suspend fun h() { }

fun f(x: Int) = x

suspend fun box() {
    // Force state machine transformation.
    h()
    for (x in 0..1) {
        f(x)
    }
    // Local `x` is NOT visible here.
    42
    for (x in 0..1) {
        f(x)
    }
}

// EXPECTATIONS JVM_IR
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:6 h: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:8 f: x:int=0:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:8 f: x:int=1:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:8 f: x:int=0:int
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:8 f: x:int=1:int
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

// EXPECTATIONS JS_IR
// test.kt:12 doResume:
// test.kt:6 h: $completion=Coroutine
// test.kt:13 doResume:
// test.kt:13 doResume:
// test.kt:13 doResume:
// test.kt:13 doResume: x=0:number
// test.kt:14 doResume: x=0:number
// test.kt:8 f: x=0:number
// test.kt:13 doResume: x=0:number
// test.kt:13 doResume: x=0:number
// test.kt:13 doResume: x=1:number
// test.kt:14 doResume: x=1:number
// test.kt:8 f: x=1:number
// test.kt:13 doResume: x=1:number
// test.kt:18 doResume: x=1:number
// test.kt:18 doResume: x=1:number
// test.kt:18 doResume: x=1:number
// test.kt:18 doResume: x=1:number, x=0:number
// test.kt:19 doResume: x=1:number, x=0:number
// test.kt:8 f: x=0:number
// test.kt:18 doResume: x=1:number, x=0:number
// test.kt:18 doResume: x=1:number, x=0:number
// test.kt:18 doResume: x=1:number, x=1:number
// test.kt:19 doResume: x=1:number, x=1:number
// test.kt:8 f: x=1:number
// test.kt:18 doResume: x=1:number, x=1:number
// test.kt:21 doResume: x=1:number, x=1:number

// EXPECTATIONS WASM
// test.kt:21 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:21 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:12 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:6 $h: $$completion:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$) (18, 18)
// test.kt:12 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4)
// test.kt:21 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:12 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:13 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (14, 4, 17, 4, 4, 14, 14, 14, 14, 14, 14)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (10, 8)
// test.kt:8 $f: $x:i32=0:i32 (16, 17)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8)
// test.kt:13 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 17, 4, 4, 14, 14, 14, 14, 14, 14)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (10, 8)
// test.kt:8 $f: $x:i32=1:i32 (16, 17)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8)
// test.kt:13 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 17, 4, 4, 4, 4)
// test.kt:17 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4)
// test.kt:18 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (14, 4, 17, 4, 4, 14, 14, 14, 14, 14, 14)
// test.kt:19 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (10, 8)
// test.kt:8 $f: $x:i32=0:i32 (16, 17)
// test.kt:19 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8)
// test.kt:18 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 17, 4, 4, 14, 14, 14, 14, 14, 14)
// test.kt:19 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (10, 8)
// test.kt:8 $f: $x:i32=1:i32 (16, 17)
// test.kt:19 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 8)
// test.kt:18 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 17, 4, 4, 4, 4)
// test.kt:21 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $x:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (0, 0, 0)
// test.kt:21 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1)
