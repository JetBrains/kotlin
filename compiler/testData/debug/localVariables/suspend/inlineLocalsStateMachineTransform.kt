// WITH_STDLIB
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

// EXPECTATIONS JVM_IR
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:8 h: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:14 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:4 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int
// test.kt:5 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int, x$iv:int=41:int
// test.kt:16 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:18 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:4 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int
// test.kt:5 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $i$f$hasLocal:int=0:int, x$iv:int=41:int
// test.kt:19 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

// EXPECTATIONS JS_IR
// test.kt:12 doResume:
// test.kt:8 h: $completion=Coroutine
// test.kt:4 doResume:
// test.kt:5 doResume: x=41:number
// test.kt:4 doResume: x=41:number
// test.kt:5 doResume: x=41:number, x=41:number
// test.kt:19 doResume: x=41:number, x=41:number
