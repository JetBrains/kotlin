
// WITH_COROUTINES
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

// The lambda object constructor has a local variables table on the IR backend.

// LOCAL VARIABLES
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:4 <init>:
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:7 foo1:

// LOCAL VARIABLES JVM
// test.kt:-1 <init>:
// LOCAL VARIABLES JVM_IR
// test.kt:-1 <init>: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation

// LOCAL VARIABLES
// test.kt:7 foo1:
// test.kt:8 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, $this$foo1:A=A, l:long=42:long
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:8 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, $this$foo1:A=A, l:long=42:long
// test.kt:9 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long, $this$foo1:A=A
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=TestKt$foo1$1
// test.kt:9 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:10 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:11 foo1: $continuation:kotlin.coroutines.Continuation=TestKt$foo1$1, $result:java.lang.Object=null, l:long=42:long
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:15 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
