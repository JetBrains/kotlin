
// WITH_COROUTINES
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

// The lambda object constructor has a local variables table on the IR backend.

// LOCAL VARIABLES
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:5 foo1:

// LOCAL VARIABLES JVM
// test.kt:-1 <init>:
// LOCAL VARIABLES JVM_IR
// test.kt:-1 <init>: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation

// LOCAL VARIABLES
// test.kt:5 foo1:
// test.kt:6 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:4 foo: $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:6 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:7 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:4 foo: $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:7 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:8 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:9 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:13 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
