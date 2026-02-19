// MODULE: library
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
// USE_INLINE_SCOPES_NUMBERS
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
// library.kt:5 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int
// library.kt:21 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, $i$f$foo\2\111:int=0:int
// library.kt:22 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, $i$f$foo\2\111:int=0:int, qfoo\2:int=2:int
// library.kt:6 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int
// test.kt:39 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int
// library.kt:11 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, $i$f$h\5\39:int=0:int
// library.kt:12 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, $i$f$h\5\39:int=0:int, xh\5:int=1:int
// library.kt:13 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, $i$f$h\5\39:int=0:int, xh\5:int=1:int, yh\5:int=2:int
// library.kt:17 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, $i$f$h\5\39:int=0:int, xh\5:int=1:int, yh\5:int=2:int, $i$f$i\6\117:int=0:int
// library.kt:18 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, $i$f$h\5\39:int=0:int, xh\5:int=1:int, yh\5:int=2:int, $i$f$i\6\117:int=0:int, zi\6:int=3:int
// library.kt:14 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, $i$f$h\5\39:int=0:int, xh\5:int=1:int, yh\5:int=2:int
// test.kt:40 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int
// test.kt:41 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int
// library.kt:29 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int, xj\7:int=4:int, $i$f$j\7\41:int=0:int
// test.kt:42 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int, xj\7:int=4:int, $i$f$j\7\41:int=0:int, yLambdaJ\8:int=4:int, xLambdaJ\8:int=3:int, $i$a$-j-TestKt$box$1$1\8\123\4:int=0:int
// test.kt:43 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int, xj\7:int=4:int, $i$f$j\7\41:int=0:int, yLambdaJ\8:int=4:int, xLambdaJ\8:int=3:int, $i$a$-j-TestKt$box$1$1\8\123\4:int=0:int, s\8:int=22:int
// library.kt:29 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int, xj\7:int=4:int, $i$f$j\7\41:int=0:int
// library.kt:30 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int, xj\7:int=4:int, $i$f$j\7\41:int=0:int
// test.kt:44 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, yLambdaG\4:int=2:int, xLambdaG\4:int=1:int, $i$a$-g-TestKt$box$1\4\114\0:int=0:int, p\4:int=12:int
// library.kt:6 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int
// library.kt:7 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int
// library.kt:25 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, $i$f$bar\3\125:int=0:int
// library.kt:26 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int, $i$f$bar\3\125:int=0:int, wbar\3:int=2:int
// library.kt:8 box: m:int=2:int, xg\1:int=0:int, $i$f$g\1\38:int=0:int
// test.kt:46 box: m:int=2:int
// test.kt:47 box: m:int=2:int, m1:int=2:int
// library.kt:5 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int
// library.kt:21 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, $i$f$foo\10\129:int=0:int
// library.kt:22 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, $i$f$foo\10\129:int=0:int, qfoo\10:int=2:int
// library.kt:6 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int
// test.kt:48 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int
// library.kt:11 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, $i$f$h\13\48:int=0:int
// library.kt:12 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, $i$f$h\13\48:int=0:int, xh\13:int=1:int
// library.kt:13 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, $i$f$h\13\48:int=0:int, xh\13:int=1:int, yh\13:int=2:int
// library.kt:17 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, $i$f$h\13\48:int=0:int, xh\13:int=1:int, yh\13:int=2:int, $i$f$i\14\135:int=0:int
// library.kt:18 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, $i$f$h\13\48:int=0:int, xh\13:int=1:int, yh\13:int=2:int, $i$f$i\14\135:int=0:int, zi\14:int=3:int
// library.kt:14 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, $i$f$h\13\48:int=0:int, xh\13:int=1:int, yh\13:int=2:int
// test.kt:49 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int
// test.kt:50 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int
// library.kt:29 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int, xj\15:int=4:int, $i$f$j\15\50:int=0:int
// test.kt:51 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int, xj\15:int=4:int, $i$f$j\15\50:int=0:int, yLambdaJ\16:int=4:int, xLambdaJ\16:int=3:int, $i$a$-j-TestKt$box$2$1\16\141\12:int=0:int
// test.kt:52 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int, xj\15:int=4:int, $i$f$j\15\50:int=0:int, yLambdaJ\16:int=4:int, xLambdaJ\16:int=3:int, $i$a$-j-TestKt$box$2$1\16\141\12:int=0:int, s2\16:int=22:int
// library.kt:29 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int, xj\15:int=4:int, $i$f$j\15\50:int=0:int
// library.kt:30 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int, xj\15:int=4:int, $i$f$j\15\50:int=0:int
// test.kt:53 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, yLambdaG\12:int=2:int, xLambdaG\12:int=1:int, $i$a$-g-TestKt$box$2\12\132\0:int=0:int, p1\12:int=12:int
// library.kt:6 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int
// library.kt:7 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int
// library.kt:25 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, $i$f$bar\11\143:int=0:int
// library.kt:26 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int, $i$f$bar\11\143:int=0:int, wbar\11:int=2:int
// library.kt:8 box: m:int=2:int, m1:int=2:int, xg\9:int=0:int, $i$f$g\9\47:int=0:int
// test.kt:54 box: m:int=2:int, m1:int=2:int
