
// WITH_STDLIB
// LANGUAGE: +JvmNullOutSpilledCoroutineLocals
// FILE: test.kt

import kotlin.coroutines.intrinsics.*

fun id(obj: Any) = obj

public suspend inline fun foo() {
    suspendCoroutineUninterceptedOrReturn<Any?> { ucont ->
        bar {
            val b = 2
            id(b)
        }
        Unit
    }
}

public inline fun bar(block: () -> Unit) {
    val c = 1
    id(c)
    block()
}

public suspend inline fun baz() {
    foo()
}

suspend fun box() {
    val a = baz()
}

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int, $i$a$-bar-TestKt$foo$2$1\5\152\3:int=0:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int, $i$a$-bar-TestKt$foo$2$1\5\152\3:int=0:int, b\5:int=2:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int, $i$a$-bar-TestKt$foo$2$1\5\152\3:int=0:int, b\5:int=2:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int, $i$a$-bar-TestKt$foo$2$1\5\152\3:int=0:int, b\5:int=2:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int, $i$f$bar\4\141:int=0:int, c\4:int=1:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int, ucont\3:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\3\140\2:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int, $i$f$foo\2\139:int=0:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$foo\2\139:int=0:int, $i$f$baz\1\31:int=0:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\31:int=0:int
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:32 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS JVM_IR
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int, b$iv$iv:int=2:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int, b$iv$iv:int=2:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int, b$iv$iv:int=2:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, ucont$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$foo:int=0:int, $i$f$baz:int=0:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:32 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a:kotlin.Unit=kotlin.Unit

// EXPECTATIONS JS_IR
// test.kt:20 doResume:
// test.kt:21 doResume: c=1:number
// test.kt:7 id: obj=1:number
// test.kt:12 doResume: c=1:number
// test.kt:13 doResume: c=1:number, b=2:number
// test.kt:7 id: obj=2:number
// test.kt:37 doResume: c=1:number, b=2:number
// test.kt:37 doResume: c=1:number, b=2:number
// test.kt:30 doResume: c=1:number, b=2:number
// test.kt:31 doResume: c=1:number, b=2:number, a=Unit

// EXPECTATIONS WASM
// test.kt:32 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:32 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:39 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=0:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17)
// test.kt:12 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=0:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (8)
// test.kt:21 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=0:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (12, 12)
// test.kt:22 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (7, 7, 7, 7, 7, 7, 4)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:22 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4, 4)
// test.kt:23 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4)
// test.kt:13 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (20, 20)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (15, 15, 15, 15, 15, 15, 12)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (12, 12, 12)
// test.kt:38 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (227, 227, 227, 227, 227, 227, 227)
// test.kt:32 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:39 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (17, 17, 17, 17, 17, 17)
// test.kt:38 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref null $kotlin.Unit)=null, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (227, 227, 227, 227, 227, 227, 227)
// test.kt:31 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref $kotlin.Unit)=(ref $kotlin.Unit), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (4, 4)
// test.kt:32 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $a:(ref $kotlin.Unit)=(ref $kotlin.Unit), $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $c:i32=1:i32, $b:i32=2:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Throwable)=null (0)
// test.kt:32 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1)
