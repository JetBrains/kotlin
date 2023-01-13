// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
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

// EXPECTATIONS JVM JVM_IR
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 foo1:
// test.kt:7 foo1: $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:5 foo: $completion:kotlin.coroutines.Continuation=A$foo1$1
// test.kt:7 foo1: $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:8 foo1: $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:5 foo: $completion:kotlin.coroutines.Continuation=A$foo1$1
// test.kt:8 foo1: $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:9 foo1: $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:10 foo1: $continuation:kotlin.coroutines.Continuation=A$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:4 <init>:
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:14 box: $completion=EmptyContinuation
// test.kt:7 doResume:
// test.kt:5 foo: $completion=$foo1COROUTINE$0
// test.kt:8 doResume:
// test.kt:5 foo: $completion=$foo1COROUTINE$0
// test.kt:9 doResume:
// test.kt:10 doResume: dead=kotlin.Long
