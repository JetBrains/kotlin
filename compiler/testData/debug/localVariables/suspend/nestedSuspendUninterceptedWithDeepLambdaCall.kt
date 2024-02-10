// IGNORE_INLINER: IR
// WITH_STDLIB
// FILE: test.kt

import kotlin.coroutines.intrinsics.*

fun id(obj: Any) = obj

private suspend inline fun foo(crossinline block: () -> Unit) {
    val a = 1
    id(a)
    suspendCoroutineUninterceptedOrReturn<Any?> { ucont ->
        val b = 2
        id(b)
        block()
        Unit
    }
}

private suspend inline fun bar(crossinline block: () -> Unit) {
    val c = 3
    id(c)
    foo(block)
}

private suspend inline fun baz(crossinline block: () -> Unit) {
    val d = 4
    id(d)
    bar(block)
}

suspend fun box() {
    val result = baz() {
        val e = 5
        id(e)
    }
}

// EXPECTATIONS JVM_IR
// test.kt:32 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:33 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:34 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int
// test.kt:35 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int, e:int=5:int
// test.kt:7 id: obj:java.lang.Object=java.lang.Integer
// test.kt:35 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int, e:int=5:int
// test.kt:36 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int, e:int=5:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// EXPECTATIONS JVM_IR
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int, $i$f$foo:int=0:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, $i$f$bar:int=0:int
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:33 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:37 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

// EXPECTATIONS JS_IR
// test.kt:27 doResume:
// test.kt:28 doResume:
// test.kt:7 id: obj=4:number
// test.kt:21 doResume:
// test.kt:22 doResume:
// test.kt:7 id: obj=3:number
// test.kt:10 doResume:
// test.kt:11 doResume:
// test.kt:7 id: obj=1:number
// test.kt:13 doResume:
// test.kt:14 doResume: b=2:number
// test.kt:7 id: obj=2:number
// test.kt:34 doResume: b=2:number
// test.kt:35 doResume: b=2:number, e=5:number
// test.kt:7 id: obj=5:number
// test.kt:42 doResume: b=2:number, e=5:number
// test.kt:42 doResume: b=2:number, e=5:number
// test.kt:33 doResume: b=2:number, e=5:number
// test.kt:37 doResume: b=2:number, e=5:number, result=Unit
