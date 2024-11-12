// API_VERSION: LATEST

// FILE: test.kt
class A

suspend fun A.foo() {}
suspend fun A.foo1(l: Long) {
    foo()
    foo()
    val dead = l
}

suspend fun box() {
    A().foo1(42)
}

// EXPECTATIONS JVM_IR
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:7 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:8 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:8 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:7 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:9 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:9 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:7 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:10 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null
// test.kt:11 foo1: $this$foo1:A=A, l:long=42:long, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, dead:long=42:long
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:15 box: $completion=EmptyContinuation
// test.kt:4 <init>:
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:8 doResume:
// test.kt:6 foo: <this>=A, $completion=Coroutine
// test.kt:9 doResume:
// test.kt:6 foo: <this>=A, $completion=Coroutine
// test.kt:10 doResume:
// test.kt:11 doResume: dead=kotlin.Long
