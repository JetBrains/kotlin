// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
// FILE: library.kt

inline fun g(xg: Int, block: (Int, Int) -> Unit) {
    foo()
    block(1, 2)
    bar()
}

inline fun h() {
    val xh = 1
    val yh = 2
    i()
}

inline fun i() {
    val zi = 3
}

inline fun foo() {
    val qfoo = 2
}

inline fun bar() {
    val wbar = 2
}

inline fun j(xj: Int, block: (Int, Int) -> Unit) {
    block(3, 4)
}

// MODULE: test(library)
// FILE: test.kt

fun box() {
    val m = 2
    g(0) { xLambdaG, yLambdaG ->
        h()
        val p = 12
        j(4) { xLambdaJ, yLambdaJ ->
            val s = 22
        }
    }

    val m1 = 2
    g(0) { xLambdaG, yLambdaG ->
        h()
        val p1 = 12
        j(4) { xLambdaJ, yLambdaJ ->
            val s2 = 22
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:37 box:
// test.kt:38 box: m:int=2:int
// library.kt:6 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// library.kt:22 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$foo\1\6:int=0:int
// library.kt:23 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$foo\1\6:int=0:int, qfoo\1$iv:int=2:int
// library.kt:7 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// test.kt:39 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int
// library.kt:12 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, $i$f$h:int=0:int
// library.kt:13 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int
// library.kt:14 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int
// library.kt:18 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int, $i$f$i\1\14:int=0:int
// library.kt:19 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int, $i$f$i\1\14:int=0:int, zi\1$iv:int=3:int
// library.kt:15 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int
// test.kt:40 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int
// test.kt:41 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int
// library.kt:30 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int
// test.kt:42 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int, yLambdaJ:int=4:int, xLambdaJ:int=3:int, $i$a$-j-TestKt$box$1$1:int=0:int
// test.kt:43 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int, yLambdaJ:int=4:int, xLambdaJ:int=3:int, $i$a$-j-TestKt$box$1$1:int=0:int, s:int=22:int
// library.kt:30 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int
// library.kt:31 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int
// test.kt:44 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$1:int=0:int, p:int=12:int
// library.kt:7 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// library.kt:8 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// library.kt:26 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$bar\2\8:int=0:int
// library.kt:27 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$bar\2\8:int=0:int, wbar\2$iv:int=2:int
// library.kt:9 box: m:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// test.kt:46 box: m:int=2:int
// test.kt:47 box: m:int=2:int, m1:int=2:int
// library.kt:6 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// library.kt:22 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$foo\1\6:int=0:int
// library.kt:23 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$foo\1\6:int=0:int, qfoo\1$iv:int=2:int
// library.kt:7 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// test.kt:48 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int
// library.kt:12 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, $i$f$h:int=0:int
// library.kt:13 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int
// library.kt:14 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int
// library.kt:18 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int, $i$f$i\1\14:int=0:int
// library.kt:19 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int, $i$f$i\1\14:int=0:int, zi\1$iv:int=3:int
// library.kt:15 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, $i$f$h:int=0:int, xh$iv:int=1:int, yh$iv:int=2:int
// test.kt:49 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int
// test.kt:50 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int
// library.kt:30 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int
// test.kt:51 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int, yLambdaJ:int=4:int, xLambdaJ:int=3:int, $i$a$-j-TestKt$box$2$1:int=0:int
// test.kt:52 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int, yLambdaJ:int=4:int, xLambdaJ:int=3:int, $i$a$-j-TestKt$box$2$1:int=0:int, s2:int=22:int
// library.kt:30 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int
// library.kt:31 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int, xj$iv:int=4:int, $i$f$j:int=0:int
// test.kt:53 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, yLambdaG:int=2:int, xLambdaG:int=1:int, $i$a$-g-TestKt$box$2:int=0:int, p1:int=12:int
// library.kt:7 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// library.kt:8 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// library.kt:26 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$bar\2\8:int=0:int
// library.kt:27 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int, $i$f$bar\2\8:int=0:int, wbar\2$iv:int=2:int
// library.kt:9 box: m:int=2:int, m1:int=2:int, xg$iv:int=0:int, $i$f$g:int=0:int
// test.kt:54 box: m:int=2:int, m1:int=2:int
