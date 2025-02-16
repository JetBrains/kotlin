// API_VERSION 2.2

// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^ This test is very flaky on JS due to a Node.js bug https://github.com/nodejs/node/issues/45410
// FILE: test.kt
class A {
    suspend fun foo() {}
    suspend fun foo1(l: Long) {
        foo()
        foo()
        val dead = l
    }
}

suspend fun box() {
    A().foo1(42)
}

// EXPECTATIONS JVM_IR
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 <init>:
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:8 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:9 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:7 foo: $completion:kotlin.coroutines.Continuation=A$foo1$1
// test.kt:9 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:10 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:7 foo: $completion:kotlin.coroutines.Continuation=A$foo1$1
// test.kt:10 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:11 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null
// test.kt:12 foo1: l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, dead:long=42:long
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
