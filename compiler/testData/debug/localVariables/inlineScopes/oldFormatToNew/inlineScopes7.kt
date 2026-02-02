// MODULE: library
// FILE: library.kt

inline fun foo(xFoo: Int, f: (Int) -> Unit, g: (Int) -> Unit) {
    bar(0, 1, 2)
    f(1)
    bar(1, 2, 3)
    g(2)
    bar(2, 3, 4)
}

inline fun bar(xBar1: Int, xBar2: Int, xBar3: Int) {
    baz(100, 101, 102)
}

inline fun baz(xBaz1: Int, xBaz2: Int, xBaz3: Int) {
    x1()
    x2()
}

inline fun x1() {
    val x1 = 1
}

inline fun x2() {
    val x2 = 2
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    foo(1, {
        val y1 = 1
        bar(0, 1, 2)
    }, {
        val y2 = 2
        bar(1, 2, 3)
    })

    foo(1, {
        val y1 = 1
        bar(0, 1, 2)
    }, {
        val y2 = 2
        bar(1, 2, 3)
    })
}

// EXPECTATIONS JVM_IR
// test.kt:34 box:
// library.kt:5 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// library.kt:13 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int
// library.kt:17 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int
// library.kt:22 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int, $i$f$x1\4\305:int=0:int
// library.kt:23 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int, $i$f$x1\4\305:int=0:int, x1\4:int=1:int
// library.kt:18 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int
// library.kt:26 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int, $i$f$x2\5\306:int=0:int
// library.kt:27 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int, $i$f$x2\5\306:int=0:int, x2\5:int=2:int
// library.kt:19 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int, xBaz1\3:int=100:int, xBaz2\3:int=101:int, xBaz3\3:int=102:int, $i$f$baz\3\301:int=0:int
// library.kt:14 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\2:int=0:int, xBar2\2:int=1:int, xBar3\2:int=2:int, $i$f$bar\2\293:int=0:int
// library.kt:6 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// test.kt:35 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int
// test.kt:36 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int
// library.kt:13 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int
// library.kt:17 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int
// library.kt:22 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int, $i$f$x1\17\320:int=0:int
// library.kt:23 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int, $i$f$x1\17\320:int=0:int, x1\17:int=1:int
// library.kt:18 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int
// library.kt:26 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int, $i$f$x2\18\321:int=0:int
// library.kt:27 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int, $i$f$x2\18\321:int=0:int, x2\18:int=2:int
// library.kt:19 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int, xBaz1\16:int=100:int, xBaz2\16:int=101:int, xBaz3\16:int=102:int, $i$f$baz\16\316:int=0:int
// library.kt:14 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int, xBar1\15:int=0:int, xBar2\15:int=1:int, xBar3\15:int=2:int, $i$f$bar\15\36:int=0:int
// test.kt:37 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\14:int=1:int, $i$a$-foo-TestKt$box$1\14\294\0:int=0:int, y1\14:int=1:int
// library.kt:6 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// library.kt:7 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// library.kt:13 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int
// library.kt:17 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int
// library.kt:22 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int, $i$f$x1\8\305:int=0:int
// library.kt:23 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int, $i$f$x1\8\305:int=0:int, x1\8:int=1:int
// library.kt:18 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int
// library.kt:26 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int, $i$f$x2\9\306:int=0:int
// library.kt:27 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int, $i$f$x2\9\306:int=0:int, x2\9:int=2:int
// library.kt:19 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int, xBaz1\7:int=100:int, xBaz2\7:int=101:int, xBaz3\7:int=102:int, $i$f$baz\7\301:int=0:int
// library.kt:14 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\6:int=1:int, xBar2\6:int=2:int, xBar3\6:int=3:int, $i$f$bar\6\295:int=0:int
// library.kt:8 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// test.kt:38 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int
// test.kt:39 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int
// library.kt:13 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int
// library.kt:17 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int
// library.kt:22 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int, $i$f$x1\22\335:int=0:int
// library.kt:23 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int, $i$f$x1\22\335:int=0:int, x1\22:int=1:int
// library.kt:18 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int
// library.kt:26 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int, $i$f$x2\23\336:int=0:int
// library.kt:27 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int, $i$f$x2\23\336:int=0:int, x2\23:int=2:int
// library.kt:19 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int, xBaz1\21:int=100:int, xBaz2\21:int=101:int, xBaz3\21:int=102:int, $i$f$baz\21\331:int=0:int
// library.kt:14 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int, xBar1\20:int=1:int, xBar2\20:int=2:int, xBar3\20:int=3:int, $i$f$bar\20\39:int=0:int
// test.kt:40 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, it\19:int=2:int, $i$a$-foo-TestKt$box$2\19\296\0:int=0:int, y2\19:int=2:int
// library.kt:8 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// library.kt:9 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// library.kt:13 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int
// library.kt:17 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int
// library.kt:22 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int, $i$f$x1\12\305:int=0:int
// library.kt:23 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int, $i$f$x1\12\305:int=0:int, x1\12:int=1:int
// library.kt:18 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int
// library.kt:26 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int, $i$f$x2\13\306:int=0:int
// library.kt:27 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int, $i$f$x2\13\306:int=0:int, x2\13:int=2:int
// library.kt:19 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int, xBaz1\11:int=100:int, xBaz2\11:int=101:int, xBaz3\11:int=102:int, $i$f$baz\11\301:int=0:int
// library.kt:14 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int, xBar1\10:int=2:int, xBar2\10:int=3:int, xBar3\10:int=4:int, $i$f$bar\10\297:int=0:int
// library.kt:10 box: xFoo\1:int=1:int, $i$f$foo\1\34:int=0:int
// test.kt:42 box:
// library.kt:5 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// library.kt:13 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int
// library.kt:17 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int
// library.kt:22 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int, $i$f$x1\27\358:int=0:int
// library.kt:23 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int, $i$f$x1\27\358:int=0:int, x1\27:int=1:int
// library.kt:18 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int
// library.kt:26 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int, $i$f$x2\28\359:int=0:int
// library.kt:27 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int, $i$f$x2\28\359:int=0:int, x2\28:int=2:int
// library.kt:19 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int, xBaz1\26:int=100:int, xBaz2\26:int=101:int, xBaz3\26:int=102:int, $i$f$baz\26\354:int=0:int
// library.kt:14 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\25:int=0:int, xBar2\25:int=1:int, xBar3\25:int=2:int, $i$f$bar\25\346:int=0:int
// library.kt:6 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// test.kt:43 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int
// test.kt:44 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int
// library.kt:13 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int
// library.kt:17 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int
// library.kt:22 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int, $i$f$x1\40\373:int=0:int
// library.kt:23 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int, $i$f$x1\40\373:int=0:int, x1\40:int=1:int
// library.kt:18 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int
// library.kt:26 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int, $i$f$x2\41\374:int=0:int
// library.kt:27 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int, $i$f$x2\41\374:int=0:int, x2\41:int=2:int
// library.kt:19 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int, xBaz1\39:int=100:int, xBaz2\39:int=101:int, xBaz3\39:int=102:int, $i$f$baz\39\369:int=0:int
// library.kt:14 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int, xBar1\38:int=0:int, xBar2\38:int=1:int, xBar3\38:int=2:int, $i$f$bar\38\44:int=0:int
// test.kt:45 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\37:int=1:int, $i$a$-foo-TestKt$box$3\37\347\0:int=0:int, y1\37:int=1:int
// library.kt:6 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// library.kt:7 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// library.kt:13 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int
// library.kt:17 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int
// library.kt:22 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int, $i$f$x1\31\394:int=0:int
// library.kt:23 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int, $i$f$x1\31\394:int=0:int, x1\31:int=1:int
// library.kt:18 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int
// library.kt:26 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int, $i$f$x2\32\395:int=0:int
// library.kt:27 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int, $i$f$x2\32\395:int=0:int, x2\32:int=2:int
// library.kt:19 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int, xBaz1\30:int=100:int, xBaz2\30:int=101:int, xBaz3\30:int=102:int, $i$f$baz\30\390:int=0:int
// library.kt:14 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\29:int=1:int, xBar2\29:int=2:int, xBar3\29:int=3:int, $i$f$bar\29\384:int=0:int
// library.kt:8 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// test.kt:46 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int
// test.kt:47 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int
// library.kt:13 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int
// library.kt:17 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int
// library.kt:22 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int, $i$f$x1\45\409:int=0:int
// library.kt:23 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int, $i$f$x1\45\409:int=0:int, x1\45:int=1:int
// library.kt:18 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int
// library.kt:26 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int, $i$f$x2\46\410:int=0:int
// library.kt:27 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int, $i$f$x2\46\410:int=0:int, x2\46:int=2:int
// library.kt:19 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int, xBaz1\44:int=100:int, xBaz2\44:int=101:int, xBaz3\44:int=102:int, $i$f$baz\44\405:int=0:int
// library.kt:14 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int, xBar1\43:int=1:int, xBar2\43:int=2:int, xBar3\43:int=3:int, $i$f$bar\43\47:int=0:int
// test.kt:48 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, it\42:int=2:int, $i$a$-foo-TestKt$box$4\42\385\0:int=0:int, y2\42:int=2:int
// library.kt:8 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// library.kt:9 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// library.kt:13 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int
// library.kt:17 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int
// library.kt:22 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int, $i$f$x1\35\428:int=0:int
// library.kt:23 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int, $i$f$x1\35\428:int=0:int, x1\35:int=1:int
// library.kt:18 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int
// library.kt:26 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int, $i$f$x2\36\429:int=0:int
// library.kt:27 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int, $i$f$x2\36\429:int=0:int, x2\36:int=2:int
// library.kt:19 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int, xBaz1\34:int=100:int, xBaz2\34:int=101:int, xBaz3\34:int=102:int, $i$f$baz\34\424:int=0:int
// library.kt:14 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int, xBar1\33:int=2:int, xBar2\33:int=3:int, xBar3\33:int=4:int, $i$f$bar\33\420:int=0:int
// library.kt:10 box: xFoo\24:int=1:int, $i$f$foo\24\42:int=0:int
// test.kt:49 box:

// EXPECTATIONS WASM
// test.kt:34 $box: $x1:i32=0:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (8, 8, 4, 4, 4)
// library.kt:5 $box: $x1:i32=0:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (8, 8, 11, 11, 14, 14, 4, 4, 4, 4, 4, 4, 4)
// library.kt:13 $box: $x1:i32=0:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=0:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=0:i32, $y2:i32=0:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=0:i32, $y2:i32=0:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=0:i32, $y2:i32=0:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=0:i32, $y2:i32=0:i32 (1, 1)
// library.kt:6 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=0:i32, $y2:i32=0:i32 (6, 6, 4, 4, 4)
// test.kt:35 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=0:i32, $y2:i32=0:i32 (17, 17)
// test.kt:36 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (12, 12, 15, 15, 18, 18, 8, 8, 8, 8, 8, 8, 8)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1, 1)
// test.kt:37 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (5, 5)
// library.kt:7 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (8, 8, 11, 11, 14, 14, 4, 4, 4, 4, 4, 4, 4)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (1, 1)
// library.kt:8 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (6, 6, 4, 4, 4)
// test.kt:38 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=0:i32 (17, 17)
// test.kt:39 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (12, 12, 15, 15, 18, 18, 8, 8, 8, 8, 8, 8, 8)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// test.kt:40 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (5, 5)
// library.kt:9 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 11, 11, 14, 14, 4, 4, 4, 4, 4, 4, 4)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:10 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// test.kt:42 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 4, 4, 4)
// library.kt:5 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 11, 11, 14, 14, 4, 4, 4, 4, 4, 4, 4)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:6 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (6, 6, 4, 4, 4)
// test.kt:43 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (17, 17)
// test.kt:44 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (12, 12, 15, 15, 18, 18, 8, 8, 8, 8, 8, 8, 8)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// test.kt:45 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (5, 5)
// library.kt:7 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 11, 11, 14, 14, 4, 4, 4, 4, 4, 4, 4)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:8 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (6, 6, 4, 4, 4)
// test.kt:46 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (17, 17)
// test.kt:47 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (12, 12, 15, 15, 18, 18, 8, 8, 8, 8, 8, 8, 8)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// test.kt:48 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (5, 5)
// library.kt:9 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 11, 11, 14, 14, 4, 4, 4, 4, 4, 4, 4)
// library.kt:13 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (8, 8, 13, 13, 18, 18, 4, 4, 4, 4, 4, 4, 4)
// library.kt:17 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:22 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:23 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// library.kt:18 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (4)
// library.kt:26 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (13, 13, 13)
// library.kt:27 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:19 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:14 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1, 1)
// library.kt:10 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
// test.kt:49 $box: $x1:i32=1:i32, $x2:i32=2:i32, $y1:i32=1:i32, $y2:i32=2:i32 (1, 1)
