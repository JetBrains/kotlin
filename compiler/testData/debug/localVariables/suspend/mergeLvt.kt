// IGNORE_INLINER: IR
// WITH_STDLIB
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

// EXPECTATIONS JVM_IR
// test.kt:26 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:20 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:7 getValue:
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int

// EXPECTATIONS ClassicFrontend FIR JVM_IR
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

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
