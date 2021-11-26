// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
// FILE: test.kt
suspend fun foo() {}
suspend fun foo1(l: Long) {
    foo()
    foo()
    val dead = l
}

suspend fun box() {
    foo1(42)
}

// EXPECTATIONS
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:5 foo1:
// test.kt:6 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:4 foo: $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:6 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:7 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:4 foo: $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:7 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:8 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:9 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
