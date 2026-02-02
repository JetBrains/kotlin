
// WITH_STDLIB
// LANGUAGE: +JvmNullOutSpilledCoroutineLocals
// FILE: test.kt

import kotlin.coroutines.intrinsics.*

class AtomicInt(val value: Int)

fun atomic(i: Int) = AtomicInt(i)

private val state = atomic(0)
private val a = 77

private inline fun AtomicInt.extensionFun() {
    if (a == 76) throw IllegalStateException("AAAAAAAAAAAA")
    value
}

private suspend inline fun suspendBar() {
    state.extensionFun()
    suspendCoroutineUninterceptedOrReturn<Any?> { ucont ->
        Unit
    }
}

suspend fun box() {
    val a = suspendBar()
}

// FIXME(JS_IR): KT-54657

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int, $this$extensionFun\2:AtomicInt=AtomicInt, $i$f$extensionFun\2\106:int=0:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int, $this$extensionFun\2:AtomicInt=AtomicInt, $i$f$extensionFun\2\106:int=0:int
// test.kt:8 getValue:
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int, $this$extensionFun\2:AtomicInt=AtomicInt, $i$f$extensionFun\2\106:int=0:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int, $this$extensionFun\2:AtomicInt=AtomicInt, $i$f$extensionFun\2\106:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2\3\113\1:int=0:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2\3\113\1:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int
// test.kt:25 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar\1\28:int=0:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS JVM_IR
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:8 getValue:
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, ucont$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, ucont$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:25 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS JS_IR
// test.kt:20 doResume:
// test.kt:11 <init properties test.kt>:
// test.kt:9 atomic: i=0:number
// test.kt:7 <init>: value=0:number
// test.kt:7 <init>: value=0:number
// test.kt:12 <init properties test.kt>:
// test.kt:11 <get-state>$accessor$1gle43a:
// test.kt:11 <get-state>:
// test.kt:15 doResume:
// test.kt:12 <get-a>$accessor$1gle43a:
// test.kt:12 <get-a>:
// test.kt:36 doResume:
// test.kt:36 doResume:
// test.kt:27 doResume:
// test.kt:28 doResume: a=Unit

// EXPECTATIONS WASM
// test.kt:29 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:29 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:21 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:10 $atomic: $i:i32=0:i32 (21, 21, 31, 21)
// test.kt:8 $AtomicInt.<init>: $<this>:(ref $AtomicInt)=(ref $AtomicInt), $value:i32=0:i32 (16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 31, 31, 31)
// test.kt:10 $atomic: $i:i32=0:i32 (33)
// test.kt:21 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 10, 10, 10)
// test.kt:16 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8, 13, 8, 8, 17)
// test.kt:17 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:37 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (153, 153, 153, 153, 153, 132, 132, 132, 132, 132, 132, 132)
// test.kt:29 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:21 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4)
// test.kt:37 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (132, 132, 132, 132, 132, 132, 132)
// test.kt:28 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref $kotlin.Unit)=(ref $kotlin.Unit), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4)
// test.kt:29 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref $kotlin.Unit)=(ref $kotlin.Unit), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (0)
// test.kt:29 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1)
