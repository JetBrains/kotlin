
// WITH_STDLIB
// LANGUAGE: +JvmNullOutSpilledCoroutineLocals
// FILE: test.kt

import kotlin.coroutines.intrinsics.*

private suspend inline fun foo() {
    suspendCoroutineUninterceptedOrReturn<Any?> { ucont ->
        Unit
    }
}

private suspend inline fun bar() {
    foo()
}

private suspend inline fun baz() {
    bar()
}

suspend fun box() {
    val a = baz()
}

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int, $i$f$bar\2\101:int=0:int
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int, $i$f$bar\2\101:int=0:int, $i$f$foo\3\102:int=0:int
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int, $i$f$bar\2\101:int=0:int, $i$f$foo\3\102:int=0:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\103\3:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int, $i$f$bar\2\101:int=0:int, $i$f$foo\3\102:int=0:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\103\3:int=0:int
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int, $i$f$bar\2\101:int=0:int, $i$f$foo\3\102:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$foo\3\102:int=0:int, $i$f$bar\2\101:int=0:int, $i$f$baz\1\23:int=0:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$bar\2\101:int=0:int, $i$f$baz\1\23:int=0:int
// test.kt:20 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\23:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS FIR JVM_IR
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$foo:int=0:int, $i$f$bar:int=0:int, $i$f$baz:int=0:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$bar:int=0:int, $i$f$baz:int=0:int
// test.kt:20 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$foo:int=0:int, $i$f$bar:int=0:int, $i$f$baz:int=0:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$bar:int=0:int, $i$f$baz:int=0:int
// test.kt:20 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS JS_IR
// test.kt:30 doResume:
// test.kt:30 doResume:
// test.kt:22 doResume:
// test.kt:23 doResume: a=Unit

// EXPECTATIONS WASM
// test.kt:24 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:24 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:31 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 116, 116, 116, 116, 116, 116, 116)
// test.kt:24 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:31 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (137, 137, 137, 137, 137, 137, 116, 116, 116, 116, 116, 116, 116)
// test.kt:23 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref $kotlin.Unit)=(ref $kotlin.Unit), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4)
// test.kt:24 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref $kotlin.Unit)=(ref $kotlin.Unit), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (0)
// test.kt:24 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1)
