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

// EXPECTATIONS JVM JVM_IR
// test.kt:25 box:
// test.kt:26 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// EXPECTATIONS JVM
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// EXPECTATIONS JVM JVM_IR
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:6 getValue:
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $this$extensionFun$iv$iv:AtomicInt=AtomicInt, $i$f$extensionFun:int=0:int
// test.kt:20 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:21 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int

// EXPECTATIONS ClassicFrontend JVM JVM_IR
// test.kt:22 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$suspendBar$2$iv:int=0:int

// EXPECTATIONS ClassicFrontend FIR JVM JVM_IR
// test.kt:20 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:23 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$suspendBar:int=0:int
// test.kt:26 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:27 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

// EXPECTATIONS JS_IR
// test.kt:19 doResume:
// test.kt:10 <init properties test.kt>:
// test.kt:8 atomic: i=0:number
// test.kt:6 <init>: value=0:number
// test.kt:6 <init>: value=0:number
// test.kt:11 <init properties test.kt>:
// test.kt:10 <get-state>$accessor$1gle43a:
// test.kt:10 <get-state>:
// test.kt:14 doResume:
// test.kt:11 <get-a>$accessor$1gle43a:
// test.kt:11 <get-a>:
// test.kt:37 doResume:
// test.kt:37 doResume:
// test.kt:26 doResume:
// test.kt:27 doResume: a=Unit
