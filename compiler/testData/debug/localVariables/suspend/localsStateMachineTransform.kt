// The JVM backend does not have `x` visible in the locals table for the for loop at all.
// IGNORE_BACKEND: JVM
// WITH_STDLIB

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

// EXPECTATIONS
// test.kt:10 box:
// test.kt:12 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:6 h: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:13 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:8 f: x:int=0:int
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:13 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:8 f: x:int=1:int
// test.kt:14 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:13 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:17 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:18 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:8 f: x:int=0:int
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:18 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=0:int
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:8 f: x:int=1:int
// test.kt:19 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:18 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, x:int=1:int
// test.kt:21 box: $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
