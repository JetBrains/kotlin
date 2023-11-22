// IGNORE_INLINER: IR
// WITH_STDLIB
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

// EXPECTATIONS JVM JVM_IR
// test.kt:29 box:
// test.kt:30 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:26 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:10 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int
// test.kt:11 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int
// test.kt:20 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int
// test.kt:21 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:21 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:22 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:12 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int
// test.kt:13 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int, b$iv$iv:int=2:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:13 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int, b$iv$iv:int=2:int
// EXPECTATIONS JVM_IR
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int, b$iv$iv:int=2:int
// EXPECTATIONS JVM
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int, $i$a$-bar-TestKt$foo$2$1$iv$iv:int=0:int
// EXPECTATIONS JVM JVM_IR
// test.kt:22 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:23 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int, $i$f$bar:int=0:int, c$iv$iv$iv:int=1:int
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int
// EXPECTATIONS ClassicFrontend JVM JVM_IR
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv:int=0:int
// EXPECTATIONS JVM JVM_IR
// test.kt:10 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int
// test.kt:17 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$foo:int=0:int
// test.kt:27 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:30 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:31 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

// EXPECTATIONS JS_IR
// test.kt:20 doResume:
// test.kt:21 doResume: c=1:number
// test.kt:7 id: obj=1:number
// test.kt:12 doResume: c=1:number
// test.kt:13 doResume: c=1:number, b=2:number
// test.kt:7 id: obj=2:number
// test.kt:38 doResume: c=1:number, b=2:number
// test.kt:38 doResume: c=1:number, b=2:number
// test.kt:30 doResume: c=1:number, b=2:number
// test.kt:31 doResume: c=1:number, b=2:number, a=Unit
