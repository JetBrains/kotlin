// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

@JvmInline
value class IntHolder(val value: Int)

suspend fun h() {}

// The suspend function view copies the parameters of the function
suspend fun suspended(a: UInt, b: IntHolder): Int {
    h()
    return a.toInt() + b.value
}

suspend fun UInt.suspendedExtension(): Int {
    h()
    return toInt()
}

suspend fun box() {
    suspended(1u, IntHolder(2))
    3u.suspendedExtension()
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:11 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:8 h: $completion:kotlin.coroutines.Continuation=TestKt$suspended$1
// test.kt:12 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:13 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:16 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:17 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1, $result:java.lang.Object=null
// test.kt:8 h: $completion:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1
// test.kt:17 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1, $result:java.lang.Object=null
// test.kt:18 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1, $result:java.lang.Object=null
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null

// EXPECTATIONS JVM
// test.kt:21 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:11 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:12 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:8 h: $completion:kotlin.coroutines.Continuation=TestKt$suspended$1
// test.kt:12 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:13 suspended-iJRGAgA: $v$c$kotlin$_UInt$$a:int=1:int, $v$c$IntHolder$$b:int=2:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:22 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:16 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:17 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1, $result:java.lang.Object=null
// test.kt:8 h: $completion:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1
// test.kt:17 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1, $result:java.lang.Object=null
// test.kt:18 suspendedExtension-qim9Vi0: $v$c$kotlin$_UInt$$$this$suspendedExtension:int=3:int, $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspendedExtension$1, $result:java.lang.Object=null
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
