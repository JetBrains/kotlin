// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

suspend fun combine(a: Int, b: Int, c: Int, block: suspend (Int, Int, Int) -> Int): Int = block(a, b, c)

fun use(x: Int) {}

suspend fun noSuspend() {}

suspend fun box() {
    combine(1, 2, 3) { v1, v2, v3 ->
        use(v1)
        use(0)
        if (v2 > 0) 0 else v2
    }
    combine(1, 2, 3) { v1, v2, v3 ->
        noSuspend()
        use(v1)
        if (v2 > 0) 0 else v2
    }
}

// EXPECTATIONS JVM
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:5 combine: a:int=1:int, b:int=2:int, c:int=3:int, block:kotlin.jvm.functions.Function4=TestKt$box$2, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 invokeSuspend: $result:java.lang.Object=kotlin.Unit
// test.kt:13 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int, v3:int=3:int
// test.kt:7 use: x:int=1:int
// test.kt:14 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int, v3:int=3:int
// test.kt:7 use: x:int=0:int
// test.kt:15 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int, v3:int=3:int
// test.kt:-1 invoke: p1:int=1:int, p2:int=2:int, p3:int=3:int, p4:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:5 combine: a:int=1:int, b:int=2:int, c:int=3:int, block:kotlin.jvm.functions.Function4=TestKt$box$2, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:5 combine: a:int=1:int, b:int=2:int, c:int=3:int, block:kotlin.jvm.functions.Function4=TestKt$box$3, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:17 invokeSuspend: $result:java.lang.Object=kotlin.Unit
// test.kt:18 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int
// test.kt:9 noSuspend: $completion:kotlin.coroutines.Continuation=TestKt$box$3
// test.kt:18 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int
// test.kt:19 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int
// test.kt:7 use: x:int=1:int
// test.kt:20 invokeSuspend: $result:java.lang.Object=kotlin.Unit, v1:int=1:int, v2:int=2:int
// test.kt:-1 invoke: p1:int=1:int, p2:int=2:int, p3:int=3:int, p4:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:5 combine: a:int=1:int, b:int=2:int, c:int=3:int, block:kotlin.jvm.functions.Function4=TestKt$box$3, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:17 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
