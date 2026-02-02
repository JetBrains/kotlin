
// WITH_STDLIB
// LANGUAGE: +JvmNullOutSpilledCoroutineLocals
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

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:33 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:34 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int
// test.kt:35 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int, $i$a$-baz-TestKt$box$result$1\5\209\0:int=0:int
// test.kt:36 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int, $i$a$-baz-TestKt$box$result$1\5\209\0:int=0:int, e\5:int=5:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:36 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int, $i$a$-baz-TestKt$box$result$1\5\209\0:int=0:int, e\5:int=5:int
// test.kt:37 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int, $i$a$-baz-TestKt$box$result$1\5\209\0:int=0:int, e\5:int=5:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int, ucont\4:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2\4\206\3:int=0:int, b\4:int=2:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz\1\34:int=0:int, d\1:int=4:int, $i$f$bar\2\200:int=0:int, c\2:int=3:int, $i$f$foo\3\203:int=0:int, a\3:int=1:int
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a\3:int=1:int, $i$f$foo\3\203:int=0:int, c\2:int=3:int, $i$f$bar\2\200:int=0:int, d\1:int=4:int, $i$f$baz\1\34:int=0:int
// test.kt:25 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, c\2:int=3:int, $i$f$bar\2\200:int=0:int, d\1:int=4:int, $i$f$baz\1\34:int=0:int
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, d\1:int=4:int, $i$f$baz\1\34:int=0:int
// test.kt:34 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:38 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, result:kotlin.Unit=kotlin.Unit

// EXPECTATIONS JVM_IR
// test.kt:33 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:34 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:35 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int
// test.kt:36 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int, e:int=5:int
// test.kt:8 id: obj:java.lang.Object=java.lang.Integer
// test.kt:36 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int, e:int=5:int
// test.kt:37 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int, $i$a$-baz-TestKt$box$result$1:int=0:int, e:int=5:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int, ucont$iv$iv$iv:kotlin.coroutines.Continuation=TestKt$box$1, $i$a$-suspendCoroutineUninterceptedOrReturn-TestKt$foo$2$iv$iv$iv:int=0:int, b$iv$iv$iv:int=2:int
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$baz:int=0:int, d$iv:int=4:int, $i$f$bar:int=0:int, c$iv$iv:int=3:int, $i$f$foo:int=0:int, a$iv$iv$iv:int=1:int
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, a$iv$iv$iv:int=1:int, $i$f$foo:int=0:int, c$iv$iv:int=3:int, $i$f$bar:int=0:int, d$iv:int=4:int, $i$f$baz:int=0:int
// test.kt:25 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, c$iv$iv:int=3:int, $i$f$bar:int=0:int, d$iv:int=4:int, $i$f$baz:int=0:int
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, d$iv:int=4:int, $i$f$baz:int=0:int
// test.kt:34 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:38 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, result:kotlin.Unit=kotlin.Unit

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

// EXPECTATIONS WASM
// test.kt:38 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context>: $<this>:(ref $EmptyContinuation)=(ref $EmptyContinuation) (37, 37)
// test.kt:38 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:28 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=0:i32, $a:i32=0:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 4, 4, 4)
// test.kt:29 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=0:i32, $a:i32=0:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (7, 7, 7, 7, 7, 7, 4)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:29 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=0:i32, $a:i32=0:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (4)
// test.kt:22 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=0:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (12, 4, 4, 4)
// test.kt:23 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=0:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (7, 7, 7, 7, 7, 7, 4)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:23 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=0:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (4)
// test.kt:11 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (12, 4, 4, 4)
// test.kt:12 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (7, 7, 7, 7, 7, 7, 4)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:12 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (4, 4, 4, 4, 4, 4)
// test.kt:44 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (82, 82, 82)
// test.kt:14 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=0:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (16, 16)
// test.kt:15 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (11, 11, 11, 11, 11, 11, 8)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:15 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (8, 8, 8)
// test.kt:16 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (8)
// test.kt:35 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (16, 16)
// test.kt:36 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (11, 11, 11, 11, 11, 11, 8)
// test.kt:8 $id: $obj:(ref $kotlin.Int)=(ref $kotlin.Int) (19, 22)
// test.kt:36 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (8, 8, 8)
// test.kt:44 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (61, 61, 61, 61, 61, 61, 61)
// test.kt:38 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=0:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (1, 1)
// test.kt:28 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (12, 12, 12, 12, 12, 12)
// test.kt:44 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref null $kotlin.Unit)=null, $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (61, 61, 61, 61, 61, 61, 61)
// test.kt:34 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref $kotlin.Unit)=(ref $kotlin.Unit), $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (4, 4)
// test.kt:38 $$boxCOROUTINE$.doResume: $<this>:(ref $$boxCOROUTINE$)=(ref $$boxCOROUTINE$), $result:(ref $kotlin.Unit)=(ref $kotlin.Unit), $d:i32=4:i32, $c:i32=3:i32, $a:i32=1:i32, $suspendResult:(ref $kotlin.Unit)=(ref $kotlin.Unit), $tmp:i32=1:i32, $b:i32=2:i32, $e:i32=5:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null (0)
// test.kt:38 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (1)
