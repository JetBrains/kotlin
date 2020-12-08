// WITH_COROUTINES
// FILE: test.kt
suspend fun h() { }

fun f(x: Int) = x

suspend fun box() {
    // Force state machine transformation.
    h()
    for (x in 0..1) {
        f(x)
    }
    // Local `x` is NOT visible here.
    42
    for (x in 0..1) {
        f(x)
    }
}

// The current backend does not have `x` visible in the locals table for the for loop at all.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:7 box:
// CoroutineUtil.kt:28 getContext:
// test.kt:-1 <init>: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:7 box:
// test.kt:9 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:3 h: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:9 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:10 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:11 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:5 f: x:int=0:int
// test.kt:11 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:10 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:11 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:5 f: x:int=1:int
// test.kt:11 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:10 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:5 f: x:int=0:int
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:5 f: x:int=1:int
// test.kt:16 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:15 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:18 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
