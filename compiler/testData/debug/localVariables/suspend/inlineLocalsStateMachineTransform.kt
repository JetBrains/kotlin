// WITH_COROUTINES
// FILE: test.kt
inline fun hasLocal(): Int {
    val x = 41
    return x + 1
}

suspend fun h() { }

suspend fun box() {
    // Force state machine transformation.
    h()
    // Local `x` should be visible in the inlined code.
    hasLocal()
    // Local `x` is not visible here.
    42
    // Local `x` (different one) is visible in the inlined code.
    hasLocal()
}

// LOCAL VARIABLES
// test.kt:10 box:
// CoroutineUtil.kt:28 getContext:

// LOCAL VARIABLES JVM_IR
// test.kt:-1 <init>: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation

// LOCAL VARIABLES JVM
// test.kt:-1 <init>:

// LOCAL VARIABLES
// test.kt:10 box:
// test.kt:12 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:8 h: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:4 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int
// test.kt:5 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int, x$iv:int=41:int
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:18 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:4 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int
// test.kt:5 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int, x$iv:int=41:int
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
