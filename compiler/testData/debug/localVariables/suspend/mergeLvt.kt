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

// EXPECTATIONS
// test.kt:25 box:
// test.kt:26 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// EXPECTATIONS JVM
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// EXPECTATIONS
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:6 getValue:
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:20 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:21 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int
// EXPECTATIONS CLASSIC_FRONTEND
// test.kt:22 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int
// EXPECTATIONS
// test.kt:20 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:23 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:26 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:27 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
