// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

@JvmInline
value class IntHolder(val value: Int)

inline fun inlined(): Int {
    val ivory: UInt = 7u
    return ivory.toInt()
}

suspend fun suspended(): Int {
    val spilled: UInt = 8u
    h()
    return spilled.toInt()
}

suspend fun h() {}

fun call(f: () -> Int): Int = f()

suspend fun box() {
    val u: UInt = 1u
    val ih = IntHolder(2)
    val boxed: UInt? = 3u
    inlined()
    call { u.toInt() + ih.value }
    suspended()
    boxed?.toInt()
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:25 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int
// test.kt:26 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt, $i$f$inlined\1\27:int=0:int
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt, $i$f$inlined\1\27:int=0:int, $v$c$kotlin$_UInt$$ivory\1:int=7:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:21 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:28 box$lambda$0: $$v$c$kotlin$_UInt$$u:int=1:int, $$v$c$IntHolder$$ih:int=2:int
// test.kt:21 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:13 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:14 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:15 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$spilled:int=8:int
// test.kt:19 h: $completion:kotlin.coroutines.Continuation=TestKt$suspended$1
// test.kt:15 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$spilled:int=8:int
// test.kt:16 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$spilled:int=8:int
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$IntHolder$$ih:int=2:int, $v$c$kotlin$_UInt$$u:int=1:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$IntHolder$$ih:int=2:int, $v$c$kotlin$_UInt$$u:int=1:int, boxed:kotlin.UInt=kotlin.UInt

// EXPECTATIONS JVM
// test.kt:23 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:24 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null
// test.kt:25 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int
// test.kt:26 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int
// test.kt:27 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt, $i$f$inlined:int=0:int
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt, $i$f$inlined:int=0:int, $v$c$kotlin$_UInt$$ivory$iv:int=7:int
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:21 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:28 box$lambda$0: $$v$c$kotlin$_UInt$$u:int=1:int, $$v$c$IntHolder$$ih:int=2:int
// test.kt:21 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:28 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:13 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1
// test.kt:14 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null
// test.kt:15 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$spilled:int=8:int
// test.kt:19 h: $completion:kotlin.coroutines.Continuation=TestKt$suspended$1
// test.kt:15 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$spilled:int=8:int
// test.kt:16 suspended: $completion:kotlin.coroutines.Continuation=TestKt$box$1, $continuation:kotlin.coroutines.Continuation=TestKt$suspended$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$spilled:int=8:int
// test.kt:29 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$kotlin$_UInt$$u:int=1:int, $v$c$IntHolder$$ih:int=2:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:30 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$IntHolder$$ih:int=2:int, $v$c$kotlin$_UInt$$u:int=1:int, boxed:kotlin.UInt=kotlin.UInt
// test.kt:31 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, $continuation:kotlin.coroutines.Continuation=TestKt$box$1, $result:java.lang.Object=null, $v$c$IntHolder$$ih:int=2:int, $v$c$kotlin$_UInt$$u:int=1:int, boxed:kotlin.UInt=kotlin.UInt
